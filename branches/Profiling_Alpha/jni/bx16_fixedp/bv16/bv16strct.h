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
  bv16strct.h : BV16 data structures

  $Log$
******************************************************************************/

#ifndef  BV16STRCT_H
#define  BV16STRCT_H

struct BV16_Decoder_State {
Word16 stsym[LPCO];
Word16 ltsym[LTMOFF];
Word16 xq[XQOFF];
Word16 lsppm[LPCO*LSPPORDER];
Word16 lgpm[LGPORDER];
Word16 lsplast[LPCO];
Word32 prevlg[2];
Word32 lmax;
Word32 lmin;
Word32 lmean;
Word32 x1;
Word32 level;
Word16 pp_last;
Word16 cfecount;
Word16 ngfae;
Word16 bq_last[3];
Word16 nggalgc;
Word16 estl_alpha_min;
UWord32 idum;
Word16 per;   /* Q15 */
Word32 E;
Word16 atplc[LPCO+1];
Word16 ma_a;
Word16 b_prv[2];
Word16 pp_prv;
};

struct BV16_Encoder_State {
Word32 prevlg[2];
Word32 lmax;
Word32 lmin;
Word32 lmean;
Word32 x1;
Word32 level;
Word16 x[XOFF];                  /* Signal memory */
Word16 xwd[XDOFF];               /* Memory of DECF:1 decimated version of xw() */
Word16 xwd_exp;                  /* or block floating-point in coarptch.c */
Word16 dq[XOFF];                 /* Q0 - Quantized short-term pred error */
Word16 dfm_h[DFO];               /* Decimated xwd() filter memory */
Word16 dfm_l[DFO];
Word16 stwpm[LPCO];             /* Q0 - Short-term weighting all-pole filter memory */
Word16 stsym[LPCO];             /* Q0 - Short-term synthesis filter memory */
Word16 stnfz[NSTORDER];          /* Q0 - Short-term noise feedback filter memory - zero section */
Word16 stnfp[NSTORDER];          /* Q0 - Short-term noise feedback filter memory - pole section */
Word16 ltnfm[MAXPP1];            /* Q0 - Long-term noise feedback filter memory */
Word16 lsplast[LPCO];
Word16 lsppm[LPCO*LSPPORDER];   /* Q15 - LSP Predictor Memory */
Word16 lgpm[LGPORDER];           /* Q11 - Log-Gain Predictor Memory */
Word16 cpplast;                  /* Pitch period pf the previous frame */
Word16 hpfzm[HPO];
Word16 hpfpm[2*HPO];
Word16 old_A[1+LPCO];           /* Q12 - LPC of previous frame */
};

struct BV16_Bit_Stream {
Word16 lspidx[2];
Word16 ppidx;
Word16 bqidx;
Word16 gidx;
Word16 qvidx[FRSZ/VDIM];
};

#endif /* BV16STRCT_H */


