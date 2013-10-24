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
  ptquan.c : Quantization of the 3 pitch predictor taps

  $Log$
******************************************************************************/

#include "typedef.h" 
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"

Word16  pitchtapquan(
                     Word16   *x,
                     Word16   pp,
                     Word16   *b,   /* Q15 */
                     Word32   *re)  /* Q3 */
{
   Word32      cormax, cor;
   Word16      s0, s1, s2;
   Word32      t0, t1, t2;
   Word16      *xt, *sp0, *sp1, *sp2;
   int         ppm2, qidx=0, i, j;
   Word32 p[9];
   Word16    sp[9];
   
   ppm2 = pp-2;
   xt   = x + XOFF;
   
   for (i=0;i<3;i++) 
   {
      sp0 = xt; 
      sp1 = x+XOFF-ppm2-i-1;
      t0  = 1;
      for (j=0;j<FRSZ;j++)
         t0 = L_mac(t0, *sp0++, *sp1++);
      p[i] = t0;
   }
   
   sp0  = x+XOFF-ppm2-3;
   s0   = *sp0++; 
   s1   = *sp0++; 
   s2   = *sp0--;
   t0   = L_mult(s0,s0); 
   p[8] = t0;
   t1   = L_mult(s0,s1); 
   p[4] = t1;
   p[5] = L_mult(s0,s2); 
   s0   = *sp0++; 
   s1   = *sp0++; 
   s2   = *sp0--;
   t2   = L_mult(s0,s0); 
   p[8] = L_add(p[8],t2); 
   p[4] = L_mac(p[4], s0, s1); 
   p[5] = L_mac(p[5], s0, s2);
   for (i=0;i<(FRSZ-2);i++) 
   {
      s0   = *sp0++; 
      s1   = *sp0++; 
      s2   = *sp0--;
      p[8] = L_mac(p[8], s0, s0);
      p[4] = L_mac(p[4], s0, s1);
      p[5] = L_mac(p[5], s0, s2);
   } 
   s0   = *sp0++; 
   s1   = *sp0++; 
   s2   = *sp0--;
   p[7] = L_mac(L_sub(p[8], t0), s0, s0);
   p[3] = L_mac(L_sub(p[4], t1), s0, s1);
   p[6] = L_mac(L_sub(p[7], t2), s1, s1);
   
   s2 = 32;
   for (i=0;i<9;i++) 
   {
      if (p[i]!=0) 
      {
         s0 = norm_l(p[i]);
         if (s0 < s2) s2 = s0;
      }
   }
   s2 =  sub(s2, 2);
   for (i=0;i<9;i++) 
      sp[i] = extract_h(L_shl(p[i],s2));
   
   cormax = MIN_32;
   sp0 = pp9cb; /* Q14 */
   for (i=0;i<PPCBSZ;i++) 
   {
      cor = 0;
      sp1 = sp;
      for (j=0;j<9;j++) 
         cor = L_mac(cor, *sp0++, *sp1++);
      if (cor > cormax) 
      {
         cormax = cor; 
         qidx   = i;
      }
   } 
   
   sp2 = pp9cb + qidx*9;
   for (i=0;i<3;i++) 
      b[i] = sp2[i];  /* multiplied by 0.5 : Q14 -> Q15 */ 
   
   sp0 = x + XOFF;
   sp1 = x + XOFF - ppm2 - 1;
   t1  = 0;
   for (i=0;i<FRSZ;i++) 
   {
      t0 = L_deposit_h(*sp0++);
      t0 = L_msu(t0, b[0], sp1[0]);
      t0 = L_msu(t0, b[1], sp1[-1]);
      t0 = L_msu(t0, b[2], sp1[-2]);
      s0 = intround(t0);      /* Q1 use as the pitch prediction residual */
      sp1++;
      t1 = L_mac(t1, s0, s0);
   }
   *re = t1;
   
   return qidx;
}
