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
  a2sp.c : Common Fixed-Point Library: conversion from a's to lsp's

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "basop32.h"
#include "mathutil.h"
#include "utility.h"

#define  NAB      (LPCO>>1)+1
#define  NBIS  4           /* number of bisections */

Word16 FNevChebP(Word16 x, Word16 *t_man, Word16 *t_exp, Word16 nd2);

void a2lsp(
           Word16 pc[],       /* (i) Q12: predictor coefficients */
           Word16 lsp[],      /* (o) Q15: line spectral pairs    */
           Word16 old_lsp[])  /* (i) Q15: old lsp                */
{
   Word16 i, j, exp;
   Word16 fa_man[NAB], fa_exp[NAB], fb_man[NAB], fb_exp[NAB];
   Word16 ta_man[NAB], ta_exp[NAB], tb_man[NAB], tb_exp[NAB];
   Word16 *t_man, *t_exp;
   Word32 a0;
   Word16 nd2, nf, ngrd;
   Word16 xroot, xlow, ylow, ind, xhigh, yhigh, xmid, ymid, dx, dy, dxdy, x, sign;


   /* Find normalization for fa and fb */
   /*   fb[0] = fa[0] = 1.0;                             */
   /*   for (i = 1, j = LPCO; i <= (LPCO/2); i++, j--) { */
   /*      fa[i] = pc[i] + pc[j] - fa[i-1];              */
   /*      fb[i] = pc[i] - pc[j] + fb[i-1];              */
   /*   }                                                */
   fa_man[0] = 16384; 
   fa_exp[0] = 6;       // fa_man[0] in high 16-bits >> fa_exp[0] = 1.0 in Q24 
   fb_man[0] = 16384;
   fb_exp[0] = 6;       // fb_man[0] in high 16-bits >> fb_exp[0] = 1.0 in Q24
   for (i = 1, j = LPCO; i <= (LPCO/2); i++, j--) {
      a0 = L_mult0(pc[i], 4096);     // Q24
      a0 = L_mac0(a0, pc[j], 4096);  // Q24
      a0 = L_sub(a0, L_shr(L_deposit_h(fa_man[i-1]),fa_exp[i-1]));  // Q24
      fa_exp[i] = norm_l(a0);
      fa_man[i] = intround(L_shl(a0, fa_exp[i]));  // Q(8+fb_exp[i])

      a0 = L_mult0(pc[i], 4096);     // Q24
      a0 = L_msu0(a0, pc[j], 4096);  // Q24
      a0 = L_add(a0, L_shr(L_deposit_h(fb_man[i-1]),fb_exp[i-1]));  // Q24
      fb_exp[i] = norm_l(a0);
      fb_man[i] = intround(L_shl(a0, fb_exp[i]));  // Q(8+fb_exp[i])
   }

   nd2 = (LPCO)/2;

   /* ta[] and tb[] in Q(7+exp)               */
   /* ta[0] = fa[nab-1]; ta[i] = 2.0 * fa[j]; */
   /* tb[0] = fb[nab-1]; tb[i] = 2.0 * fb[j]; */
   ta_man[0] = fa_man[NAB-1];
   ta_exp[0] = add(fa_exp[NAB-1], 1);
   tb_man[0] = fb_man[NAB-1];
   tb_exp[0] = add(fb_exp[NAB-1], 1);
   for (i = 1, j = NAB - 2; i < NAB; ++i, --j) {
      ta_man[i] = fa_man[j];
      ta_exp[i] = fa_exp[j];
      tb_man[i] = fb_man[j];
      tb_exp[i] = fb_exp[j];
   }

   nf = 0;
   t_man = ta_man;
   t_exp = ta_exp;
   xroot = 0x7fff;
   ngrd  = 0;
   xlow  = grid[0];  // Q15
   ylow = FNevChebP(xlow, t_man, t_exp, nd2);
   ind = 0;

   /* Root search loop */
   while (ngrd<(Ngrd-1) && nf < LPCO) {
      
      ngrd++;
      xhigh = xlow;
      yhigh = ylow;
      xlow  = grid[ngrd];
      ylow = FNevChebP(xlow, t_man, t_exp, nd2);
      
      if ( L_mult(ylow ,yhigh) <= 0) {
         
         /* Bisections of the interval containing a sign change */
         
         dx = xhigh - xlow;
         for (i = 1; i <= NBIS; ++i) {
            dx = shr(dx, 1);
            xmid = add(xlow, dx);
            ymid = FNevChebP(xmid, t_man, t_exp, nd2);
            if (L_mult(ylow,ymid) <= 0) {
               yhigh = ymid;
               xhigh = xmid;
            } else {
               ylow = ymid;
               xlow = xmid;
            }
         }
         
         /*
         * Linear interpolation in the subinterval with a sign change
         * (take care if yhigh=ylow=0)
         */
         
         dx = sub(xhigh, xlow);
         dy  = sub(ylow, yhigh);
         if (dy != 0) {
            sign = dy;
            dy = abs_s(dy);
            exp = norm_s(dy);
            dy = shl(dy, exp);
            /* The maximum grid distance is 1629 =>                                  */
            /* Maximum dx=1629/2^4=101.8125, i.e. 16384/101.8125=160.92~128 (7 bits) */
            /* However, due to the starting point for the search of a new root,      */
            /* xlow = xroot, 1 more bit of headroom for the division is required.    */
            dxdy = div_s(shl(dx,6), dy); 
            a0 = L_mult(dxdy, ylow);
            a0 = L_shr(a0, sub(6, exp));
            x  = intround(a0);
            if(sign < 0) x = negate(x);
            xmid = add(xlow, x);
         } 
         else {
            xmid = add(xlow, shr(dx,1));
         }
         
         /* acos mapping for New lsp component */
         while (( costable[ind] >= xmid ) && (ind < 63)) ind++;
         ind--;
         a0 = L_mult( sub(xmid, costable[ind]) , acosslope[ind] );
         x  = intround(L_shl(a0, 4));
         lsp[nf] = add(x, shl(ind, 9));
         ++nf;
         
         /* Start the search for the roots of next polynomial at the estimated
         * location of the root just found.  We have to catch the case that the
         * two polynomials have roots at the same place to avoid getting stuck at
         * that root.
         */
         
         if (xmid >= xroot) xmid = xlow - dx;
         xroot = xmid;
         if (t_man == ta_man){
            t_man = tb_man;
            t_exp = tb_exp;
         }
         else{
            t_man = ta_man;
            t_exp = ta_exp;
         }
         xlow = xmid;
         ylow = FNevChebP(xlow, t_man, t_exp, nd2);
         
      }
   }
   
   /* Check if all LSPs are found */
   if( sub(nf, LPCO) < 0)
   {
      W16copy(lsp, old_lsp, LPCO);
   }

   return;
}

Word16 FNevChebP(
                 Word16 x,       /* (i) Q15: value 	               */
                 Word16 *t_man,  /* (i) Q7: mantissa of coefficients */
                 Word16 *t_exp,  /* (i): exponent fo cofficients     */
                 Word16 nd2)     /* (i): order                       */
{
   Word16 i;
   Word16 x2;
   Word16 b_man[NAB], b_exp[NAB];
   Word16 y;
   Word32 a0;

   x2 = x;                                                           // 2x in Q14
   b_man[0] = t_man[nd2];
   b_exp[0] = t_exp[nd2];                                            // b[0] in Q(7+t_exp)
   a0 = L_mult(x2, b_man[0]);
   a0 = L_shr(a0, sub(b_exp[0], 1));                                 // t*b[0] in Q23
   a0 = L_add(a0, L_shr(L_deposit_h(t_man[nd2-1]), t_exp[nd2-1]));   // c[nd2-1] + t*b[0] in Q23
   b_exp[1] = norm_l(a0);
   b_man[1] = intround(L_shl(a0, b_exp[1]));                            // b[1] = c[nd2-1] + t * b[0]

   for (i=2;i<nd2;i++){
      a0 = L_mult(x2, b_man[i-1]);
      a0 = L_shr(a0, sub(b_exp[i-1], 1));                            // t*b[i-1] in Q23
      a0 = L_add(a0, L_shr(L_deposit_h(t_man[nd2-i]), t_exp[nd2-i]));// c[nd2-i] + t*b[i-1] in Q23
      a0 = L_sub(a0, L_shr(L_deposit_h(b_man[i-2]), b_exp[i-2]));    // c[nd2-i] + t*b[i-1] - b[i-2] in Q23
      b_exp[i] = norm_l(a0);
      b_man[i] = intround(L_shl(a0, b_exp[i]));                         // b[i] = c[nd2-i] - b[i-2] + t * b[i-1]
   }

   a0 = L_mult(x, b_man[nd2-1]);
   a0 = L_shr(a0, b_exp[nd2-1]);                                     // x*b[nd2-1] in Q23
   a0 = L_add(a0, L_shr(L_deposit_h(t_man[0]), t_exp[0]));           // c[0] + x*b[nd2-1] in Q23
   a0 = L_sub(a0, L_shr(L_deposit_h(b_man[nd2-2]), b_exp[nd2-2]));   // c[0] + x*b[nd2-1] - b[nd2-2] in Q23

   y = intround(L_shl(a0, 6));                                          // Q13

   return y;                                                         // Q13
}
