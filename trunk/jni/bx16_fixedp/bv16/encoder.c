/*****************************************************************************/
/* BroadVoice(R)16 (BV16) Fixed-Point ANSI-C Source Code                     */
/* Revision Date: November 13, 2009                                          */
/* Version 1.1                                                               */
/*****************************************************************************/

/*****************************************************************************/
/* Copyright 2000-2009 Broadcom Corporation                                  */
/*                                                                           */
/* This software is provided under the GNU Lesser General Public License,    */
/* version 2.1, as published by the Free Software Foundation ("LGPL").       */
/* This program is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY SUPPORT OR WARRANTY; without even the implied warranty of     */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the LGPL for     */
/* more details.  A copy of the LGPL is available at                         */
/* http://www.broadcom.com/licenses/LGPLv2.1.php,                            */
/* or by writing to the Free Software Foundation, Inc.,                      */
/* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 */
/*****************************************************************************/


/*****************************************************************************
  encoder.c : BV16 Fixed-Point Encoder Main Subroutines

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"
#include "utility.h"

void Reset_BV16_Encoder(struct BV16_Encoder_State *c)
{
   W16zero((Word16 *) c, sizeof(struct BV16_Encoder_State)/sizeof(Word16));
   c->lsplast[0]  =   3641;
   c->lsplast[1]  =   7282;
   c->lsplast[2]  =  10923;
   c->lsplast[3]  =  14564;
   c->lsplast[4]  =  18204;
   c->lsplast[5]  =  21845; 
   c->lsplast[6]  =  25486;
   c->lsplast[7]  =  29127;
   c->xwd_exp     = 32;
   c->cpplast     = 12*DECF;
   c->lmax        = MIN_32;
   c->lmin        = MAX_32;
   c->lmean       = 419430400;     /* 12.5 Q25 */
   c->x1          = 570425344;        /* 17.0 Q25 */
   c->level       = 570425344;     /* 17.0 Q25 */
   c->old_A[0]    = 4096;
}

void BV16_Encode(
                 struct BV16_Bit_Stream      *bs,
                 struct BV16_Encoder_State   *cs,
                 Word16 *inx)
{
   Word32 r[NSTORDER+1];  /* DPF representation of autocorrelation lags */
   Word16 ltnfm[MAXPP1+FRSZ];
   Word16 a[LPCO+1];  /* LPC coefficients */
   Word16 aw[LPCO+1]; /* Weighted LPC coefficients */
   Word16 fsz[NSTORDER+1];
   Word16 fsp[NSTORDER+1];
   Word16 x[LX];        /* Signal buffer */
   Word16 dq[LX];    /* quantized short term pred error */
   Word16 sdq[LX];
   Word16 xw[FRSZ];     /* Q0 perceptually weighted version of x() */
   Word16 lsp[LPCO], lspq[LPCO];     /* Q15 */
   Word16 cbs[VDIM*CBSZ];
   Word16 bq[3];     /* Q15 */
   Word16 beta;      /* Q13 */   
   Word16 ppt;    /* Q9 */
   Word16 gainq;     /* Q2 */ 
   Word32 ee;     /* Q1 */
   Word32 a0;  
   Word16 gain_exp;
   Word16 pp, cpp, i;
   Word16 eehi, eelo;
   Word16 a0hi, a0lo;
   Word16 dummy;
   
   /* copy state memory to local memory buffers */
   W16copy(x, cs->x, XOFF);
   W16copy(ltnfm, cs->ltnfm, MAXPP1);
   
   /* 150 Hz HighPass filtering */
   preprocess(cs, x+XOFF, inx, FRSZ);
   
   /* update highpass filtered signal buffer */
   W16copy(cs->x,x+FRSZ,XOFF);
   
   /* perform lpc analysis with asymmetrical window */
   Autocorr(r, x+LX-WINSZ, winl, WINSZ, NSTORDER);
   Spectral_Smoothing(NSTORDER, r, sstwinl_h, sstwinl_l);
   Levinson(r, a, cs->old_A, LPCO);
   
   /* pole-zero noise feedback filter */
   
   fsz[0] = 0;
   for (i=1;i<=NSTORDER;i++) 
      fsz[i] = mult_r(a[i], gfsz[i]);
   fsp[0] = 4096;
   for (i=1;i<=NSTORDER;i++) 
      fsp[i] = mult_r(a[i], gfsp[i]);
   
   /* bandwidth expansion */
   for (i=1;i<=LPCO;i++) 
      a[i] = mult_r(bwel[i],a[i]);
   
   /* LPC -> LSP Conversion */
   a2lsp(a,lsp,cs->lsplast);
   W16copy(cs->lsplast,lsp,LPCO);
   
   /* Spectral Quantization */
   lspquan(lspq,bs->lspidx,lsp,cs->lsppm);
   lsp2a(lspq,a);
   
   /* calculate lpc prediction residual */
   W16copy(dq,cs->dq,XOFF);
   azfilter(a,LPCO,x+XOFF,dq+XOFF,FRSZ);
   
   /* weighted version of lpc filter to generate weighted speech */
   
   aw[0] = a[0];
   for (i=1;i<=LPCO; i++) aw[i] = mult_r(STWAL[i],a[i]);
   
   /* get perceptually weighted speech signal */
   apfilter(aw, LPCO, dq+XOFF, xw, FRSZ, cs->stwpm, 1); 
   
   /* get the coarse version of pitch period using 4:1 decimation */
   cpp = coarsepitch(xw, cs);
   cs->cpplast=cpp;
   
   /* refine the pitch period in the neighborhood of coarse pitch period
   also calculate the pitch predictor tap for single-tap predictor */
   
   for (i=0;i<LX;i++) 
      sdq[i] = shr(dq[i],2);
   pp = refinepitch(sdq, cpp, &ppt);
   bs->ppidx = pp - MINPP;
   
   /* vector quantize 3 pitch predictor taps with minimum residual energy */
   bs->bqidx=pitchtapquan(dq, pp, bq, &ee);
   
   /* scale up pitch prediction residual energy by 1.5^2=2.25 (18432 Q13) */
   L_Extract(ee, &eehi, &eelo);
   ee = L_shl(Mpy_32_16(eehi, eelo, 18432), 2);
   
   /* get coefficients of long-term noise feedback filter */
   if (ppt > 512) 
      beta = LTWF;
   else if (ppt <= 0) 
      beta = 0;
   else 
      beta = extract_h(L_shl(L_mult(LTWF, ppt),6)); 
   
   /* gain quantization */
   bs->gidx = gainquan(&a0,&ee,cs->lgpm,cs->prevlg,cs->level);
   
   /* level estimation */
   dummy = estl_alpha;
   estlevel(cs->prevlg[0],&cs->level,&cs->lmax,&cs->lmin,
      &cs->lmean,&cs->x1,LGPORDER+1,Nfdm+1,&dummy);
   
   /* find appropriate gain block floating point exponent */
   gain_exp = sub(norm_l(a0), 2);
   /* scale down quantized gain by 1.5, 1/1.5=2/3 (21845 Q15) */
   L_Extract(a0, &a0hi, &a0lo);
   a0 = Mpy_32_16(a0hi, a0lo, 21845);  
   gainq  = intround(L_shl(a0, gain_exp));
   
   /* scale the scalar quantizer codebook */
   for (i=0;i<(VDIM*CBSZ);i++) 
      cbs[i] = mult_r(gainq, cccb[i]);
   
   /* perform noise feedback coding of the excitation signal */
   excquan(bs->qvidx,x+XOFF,a,fsz,fsp,bq,beta,cs->stsym,
      dq,ltnfm,cs->stnfz,cs->stnfp,cbs,pp,gain_exp);
   
   /* update state memory */
   W16copy(cs->dq,&dq[FRSZ],XOFF);
   W16copy(cs->ltnfm,&ltnfm[FRSZ],MAXPP1);
   
}
