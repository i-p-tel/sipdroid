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
  levdur.c : Common Fixed-Point Library: Levinson-Durbin
  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "basop32.h"

void Levinson(
  Word32 r32[],    /* (i)  : r32[] double precision vector of autocorrelation coefficients   */
  Word16 a[],      /* (o)  : a[] in Q12 - LPC coefficients                                   */
  Word16 old_a[],  /* (i/o): old_a[] in Q12 - previous LPC coefficients                      */
  Word16 m	       /* (i)  : LPC order                                                       */
)
{
   Word16 i, j, high, low, alpha_hi, alpha_lo, alpha_exp;
   Word16 exp, r_hi[LPCO+1], r_lo[LPCO+1];
   Word16 a_hi[LPCO+1], a_lo[LPCO+1], anew_hi[LPCO+1], anew_lo[LPCO+1];
   Word16 rc_hi, rc_lo;
   Word32 a0, a1, alpha_man;

   /* Normalization of autocorrelation coefficients */
   exp = norm_l(r32[0]);
   for(i=0; i<=m; i++){
      r32[i] = L_shl(r32[i], exp);
      L_Extract(r32[i], r_hi+i, r_lo+i);
   }

   /* a[1] = rc = -r[1]/r[0] */
   a1 = L_abs(r32[1]);
   a0 = Div_32(a1, r_hi[0], r_lo[0]);        // rc in Q31
   if(r32[1] > 0)
      a0 = L_negate(a0);
   L_Extract(L_shr(a0,4), a_hi+1, a_lo+1);   // Q27
   
   /*  alpha = r[0]*(1-rc*rc) */
   L_Extract(a0, &high, &low);
   a0 = Mpy_32(high, low, high, low);        // rc^2 in Q31
   a0 = L_abs(a0);                           // Lesson from G.729
   a0 = L_sub(0x40000000, L_shr(a0,1));      // 1-rc*rc in Q30
   L_Extract(a0, &high, &low);
   a0 = Mpy_32(r_hi[0], r_lo[0], high, low); // alpha in Q30
   alpha_exp = norm_l(a0);
   alpha_man = L_shl(a0, alpha_exp);
   alpha_exp = sub(alpha_exp, 1);            // alpha: Q(31+alpha_exp)
   
   /* Recursive solution of Yule-Walker equations */
   for(i= 2; i<=m; i++)
   {
      
      /* s =  r[i] + sum{r[j]*a[i-j], j=1,2,...,i-1} */
      a0 = 0;
      for(j=1; j<i; j++) {
         a1 = Mpy_32(r_hi[j], r_lo[j], a_hi[i-j], a_lo[i-j]);  // Q27
         a0 = L_add(a0, a1);                                   // Q27
      }
      a0 = L_shl(a0,4);                                  // Q31
      a0 = L_add(a0, r32[i]);                            // Q31

      /* rc = -s/alpha */
      exp = norm_l(a0);
      a0 = L_shl(a0, exp);
      a1 = L_abs(a0);
      if(L_sub(a1, alpha_man) >= 0){
         a1 = L_shr(a1,1);
         exp = sub(exp,1);
      }
      L_Extract(alpha_man, &alpha_hi, &alpha_lo);
      a1 = Div_32(a1, alpha_hi, alpha_lo);
      if(a0 > 0)
         a1 = L_negate(a1);                     // rc in Q(31+exp-alpha_exp)
      a1 = L_shr(a1, sub(exp, alpha_exp));      // rc in Q31
      L_Extract(a1, &rc_hi, &rc_lo);            // rc in Q31

      /* Check for absolute value of reflection coefficient - stability */
      if (sub(abs_s(intround(a1)), 32750) > 0)
      {
         a[0] = 4096;
         for(j=1; j<=m; j++)
            a[j] = old_a[j];

         return;
      }
      
      /* anew[j]=a[j]+rc*a[i-j], j=1,2,...i-1 */
      /* anew[i]=rc                           */
      for(j=1; j<i; j++)
      {
         a0 = Mpy_32(a_hi[i-j], a_lo[i-j], rc_hi, rc_lo);   // Q27
         a0 = L_add(a0, L_Comp(a_hi[j], a_lo[j]));          // Q27
         L_Extract(a0, anew_hi+j, anew_lo+j);               // Q27
      }
      L_Extract(L_shr(a1, 4), anew_hi+i, anew_lo+i);        // Q27

      /* alpha = alpha*(1-rc*rc) */
      a0 = Mpy_32(rc_hi, rc_lo, rc_hi, rc_lo);     // rc*rc in Q31
      a0 = L_abs(a0);                              // Lesson from G.729
      a0 = L_shr(a0, 1);                           // rc*rc in Q30
      a0 = L_sub(0x40000000, a0);                  // 1-rc*rc in Q30
      L_Extract(a0, &high, &low);
      a0 = Mpy_32(alpha_hi, alpha_lo, high, low);  // Q(30+alpha_exp)
      exp = norm_l(a0);
      alpha_man = L_shl(a0, exp);                  // Q(30+exp+alpha_exp)
      alpha_exp = sub(add(alpha_exp, exp), 1);     // alpha: Q(31+alpha_exp)
      
      /* a[j] = anew[j] in Q(12+a1_exp) */
      for(j=1; j<=i; j++)
      {
         a_hi[j] = anew_hi[j];
         a_lo[j] = anew_lo[j];
      }
   }

   /* convert lpc coefficients to Q12 and save new lpc as old lpc for next frame */
   a[0] = 4096;
   for(j=1; j<=m; j++)
   {
      a[j] = intround(L_shl(L_Comp(a_hi[j], a_lo[j]), 1));  // Q12
      old_a[j] = a[j];
   }

   return;
}
