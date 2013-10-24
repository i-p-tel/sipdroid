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
  decoder.c : BV16 Fixed-Point Decoder Main Subroutines

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"
#include "utility.h"
#include "mathutil.h"
#include "postfilt.h"

void Reset_BV16_Decoder(struct BV16_Decoder_State *c)
{
   int   i;
   W16zero((Word16 *) c, sizeof(struct BV16_Decoder_State)/sizeof(Word16));
   c->lsplast[0] =   3641;
   c->lsplast[1] =   7282;
   c->lsplast[2] =  10923;
   c->lsplast[3] =  14564;
   c->lsplast[4] =  18204;
   c->lsplast[5] =  21845;
   c->lsplast[6] =  25486;
   c->lsplast[7] =  29127;
   c->pp_last  = 50;
   c->lmax     = MIN_32;
   c->lmin     = MAX_32;
   c->lmean    = 419430400;      /* 12.5 Q25 */
   c->x1       = 570425344;      /* 17.0 Q25 */
   c->level    = 570425344;      /* 17.0 Q25 */
   c->ngfae    = LGPORDER+1;
   c->nggalgc  = Nfdm+1;
   c->estl_alpha_min = estl_alpha;
   c->idum  =0;
   c->per   = 0;
   for(i=0; i<LPCO; i++)
      c->atplc[i+1] = 0;
   c->ma_a     = 0;
   c->b_prv[0] = 8192; /* Q13 */
   c->b_prv[1] = 0;
   c->pp_prv   = 100;
}

void BV16_Decode(
                 struct BV16_Bit_Stream     *bs,
                 struct BV16_Decoder_State  *ds,
                 Word16    *x)
{
   Word32 lgq, lg_el;
   Word16 gainq;         /* Q3 */
   Word16 pp;
   Word32 a0;
   Word16 gain_exp;
   Word16 i;
   Word16 a0hi, a0lo;
   Word16 ltsym[LTMOFF+FRSZ];
   Word16 xq[LXQ];
   Word16 a[LPCO+1];
   Word16 lspq[LPCO];       /* Q15 */
   Word16 cbs[VDIM*CBSZ];
   Word16 bq[3];         /* Q15 */
   Word32 bss;
   Word32  E;
   
   /* set frame erasure flags */
   if (ds->cfecount != 0) {
      ds->ngfae = 1;
   } else {
      ds->ngfae++;
      if (ds->ngfae>LGPORDER) ds->ngfae=LGPORDER+1;
   }
   
   /* reset frame erasure counter */
   ds->cfecount = 0;
   
   /* decode pitch period */
   pp = (bs->ppidx + MINPP);
   
   /* decode spectral information */
   lspdec(lspq,bs->lspidx,ds->lsppm,ds->lsplast);
   lsp2a(lspq,a);
   W16copy(ds->lsplast, lspq, LPCO);
   
   /* decode pitch taps */
   pp3dec(bs->bqidx, bq);
   
   /* decode gain */
   a0 = gaindec(&lgq,bs->gidx,ds->lgpm,ds->prevlg,ds->level,
      &ds->nggalgc,&lg_el);
   
   /* gain normalization */
   gain_exp = sub(norm_l(a0), 2);
   /* scale down quantized gain by 1.5, 1/1.5=2/3 (21845 Q15) */
   L_Extract(a0, &a0hi, &a0lo);
   a0 = Mpy_32_16(a0hi, a0lo, 21845);  
   gainq = intround(L_shl(a0, gain_exp));
   
   
   /* scale the scalar quantizer codebook to current signal level */
   for (i=0;i<(VDIM*CBSZ);i++) cbs[i] = mult_r(gainq, cccb[i]);
   
   /* copy state memory to buffer */
   W16copy(xq, ds->xq, XQOFF);
   W16copy(ltsym, ds->ltsym, LTMOFF);
   
   /* decoding of the excitation signal with integrated long-term */
   /* and short-term synthesis */
   excdec_w_synth(xq+XQOFF,ltsym+LTMOFF,ds->stsym,bs->qvidx,bq,cbs,pp,
      a,gain_exp,&E);
   
   ds->E = E;
   
   /* update the remaining state memory */
   W16copy(ds->ltsym, ltsym+FRSZ, LTMOFF);
   W16copy(ds->xq, xq+FRSZ, XQOFF);
   ds->pp_last = pp;
   W16copy(ds->bq_last, bq, 3);
   
   /* level estimation */
   estlevel(lg_el,&ds->level,&ds->lmax,&ds->lmin,&ds->lmean,&ds->x1,
      ds->ngfae, ds->nggalgc,&ds->estl_alpha_min);

   /* adaptive postfiltering */
   postfilter(xq, pp, &(ds->ma_a), ds->b_prv, &(ds->pp_prv), x);

   /* scale signal up by 1.5 */
   for(i=0; i<FRSZ; i++)
      x[i] = add(x[i], shr(x[i],1));
   
   W16copy(ds->atplc, a, LPCO+1);
   bss = L_add(L_add(bq[0], bq[1]), bq[2]);
   if (bss > 32768)
      bss = 32768;
   else if (bss < 0)
      bss = 0;
   ds->per = add(shr(ds->per, 1), (Word16)L_shr(bss, 1));
   
}
