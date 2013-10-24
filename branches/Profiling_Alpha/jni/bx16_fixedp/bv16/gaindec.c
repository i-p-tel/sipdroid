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
  gaindec.c : gain decoding functions

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"
#include "mathutil.h"

Word32 gaindec(   /* Q18 */
               Word32   *lgq,    /* Q25 */
               Word16   gidx,
               Word16   *lgpm,   /* Q11 */
               Word32   *prevlg, /* Q25 */
               Word32   level,   /* Q25 */
               Word16   *nggalgc,
               Word32   *lg_el)
{
   Word32   elg;
   Word16   lg_exp, lg_frac, lgc;
   Word16   i, n, k;
   
   /* Calculate estimated log-gain */
   elg = L_shr(L_deposit_h(lgmean),1);          /* Q26 */
   for (i = 0; i < LGPORDER; i++) {
      elg = L_mac0(elg, lgp[i],lgpm[i]);        /* Q26 */
   }
   elg = L_shr(elg,1);
   
   /* Calculate decoded log-gain */
   *lgq = L_add(L_shr(L_deposit_h(lgpecb[gidx]), 2), elg);  /* Q25 */
   
   /* Look up from lgclimit() table the maximum log gain change allowed */
   i = shr(sub(shr(extract_h(L_sub(prevlg[0],level)),9),LGLB),1); 
   /* get column index */
   if (i >= NGB) {
      i = NGB - 1;
   } else if (i < 0) {
      i = 0;
   }
   n = shr(sub(shr(extract_h(L_sub(prevlg[0],prevlg[1])),9),LGCLB),1); 
   /* get row index */
   if (n >= NGCB) {
      n = NGCB - 1;
   } else if (n < 0) {
      n = 0;
   }
   
   i = i * NGCB + n;
   
   /* Update log-gain predictor memory,
   check whether decoded log-gain exceeds lgclimit */
   for (k = LGPORDER - 1; k > 0; k--) {
      lgpm[k] = lgpm[k-1];
   }
   lgc = extract_h(L_sub(*lgq, prevlg[0]));     /* Q9 */
   if ((lgc>lgclimit[i]) && (gidx>0)) { /* if log-gain exceeds limit */
      *lgq = prevlg[0]; /* use the log-gain of previous frame */
      lgpm[0] = extract_h(L_shl(L_sub(*lgq, elg),2));
      *nggalgc = 0;
      *lg_el = L_add(L_deposit_h(lgclimit[i]), prevlg[0]);
   } else {
      lgpm[0] = lgpecb[gidx];
      *nggalgc = add(*nggalgc, 1);
      if (*nggalgc > Nfdm) *nggalgc = Nfdm+1;
      *lg_el = *lgq;
   }
   
   /* Update log-gain predictor memory */
   prevlg[1] = prevlg[0];
   prevlg[0] = *lgq;
   
   /* Convert quantized log-gain to linear domain */
   elg = L_shr(*lgq,10);            /* Q25 -> Q26 (0.5F) --> Q16 */
   L_Extract(elg, &lg_exp, &lg_frac);
   lg_exp = add(lg_exp, 18);        /* output to be Q2 */
   return Pow2(lg_exp, lg_frac);
}

void gainplc(Word32 E, Word16 *lgeqm, Word32 *lgqm)
{
   int k;
   Word16 exponent, fraction, lge;
   Word32 lg, mrlg, elg;
   
   exponent = 1;
   fraction = 0;
   if (E > TMinlg*FRSZ)
   {
      Log2( E, &exponent, &fraction);
      lg = L_add(L_shl(L_deposit_h(exponent),9), 
         L_shr(L_deposit_h(fraction),6));    /* Q25 */
      lg = L_sub(lg, 178574274); /* 178574274 = log2(1/SFSZL) Q 25 */
   }
   else
      lg = 0;   /* Minlg */
   
   mrlg = L_shr(L_deposit_h(lgmean),2); /* Q25 */
   mrlg = L_sub(lg, mrlg);  /* Q25 */
   
   elg = 0;
   for(k=0; k<GPO; k++)
      elg = L_mac0(elg, lgp[k], lgeqm[k]);  /* Q26 */
   
   elg = L_shr(elg,1);                    /* Q25 */
   
   /* predicted log-gain error */
   lge = intround(L_shl(L_sub(mrlg, elg),2));   /* Q11 */
   
   /* update quantizer memory */
   for(k=GPO-1; k>0; k--)
      lgeqm[k] = lgeqm[k-1];
   lgeqm[0] = lge;
   
   /* update quantized log-gain memory */
   lgqm[1] = lgqm[0];
   lgqm[0] = lg;
   
   return;
}
