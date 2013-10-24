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
  dspfunc.c : Common Fixed-Point Library: common signal processing functions

  $Log$
******************************************************************************/

#include "typedef.h"
#include "basop32.h"
#include "mathutil.h"

/****************************************************/
/* y = 2^x = 2^exp * 2^frac                         */
/*   = 2^(exp+1) * 2^(frac-1)                       */
/*                                                  */
/* 2^(exp+1)  = 1<<(exp+1)                          */
/* 2^(frac-1) = 2^z, z in [-1; 0[ use table look-up */
/*              and interpolation.                  */
/****************************************************/

Word32 Pow2(                  /* Q0 output            */
            Word16 int_comp,  /* Q0 Integer part      */
            Word16 frac_comp  /* Q15 frac_compal part  */
            )
{
   Word32 a0;
   Word16 idx_frac, frac, sub_frac, sub_tab;

   idx_frac = shr(frac_comp, 9);                 // for 65 entry table
   sub_frac = frac_comp & 0x01FF;                // sub-frac_comp in Q9
   sub_frac = shl(sub_frac, 6);                 // Q15
   frac = tabpow[idx_frac];                     // Q15 table entry of 2^(frac_comp-1)
   a0 = L_deposit_h(frac);                      // Q31 table entry of 2^(frac_comp-1)
   sub_tab = sub(tabpow[idx_frac+1], frac);     // Q15 difference to next table entry
   a0 = L_mac(a0, sub_frac, sub_tab);           // Q31 linear interpolation between table entries

   a0 = L_shr_r(a0, sub(30, int_comp));         // Q0 - note: sub(30, int_comp) = 31-(int_comp+1)

   return a0;
}

/***********************************************************/
/* y = log2(x) = log2(man*2^exp)                           */
/*   = log2(man) + log2(2^exp)                             */
/*   = log2(man) + exp                                     */
/*                                                         */
/* exponent = 30-exp                                       */
/* fraction = table look-up and interpolation of log2(man) */
/***********************************************************/

void Log2(
          Word32 x,         /* (i) input           */
          Word16 *int_comp, /* Q0 integer part     */
          Word16 *frac_comp /* Q15 fractional part */
          )
{
   Word16 exp, idx_man, sub_man, sub_tab;
   Word32 a0;

   if(x <= 0){
      *int_comp = 0;
      *frac_comp = 0;
   }
   else{
      exp = norm_l(x);                                   // normalization
      a0 = L_shl(x, exp);                                // Q30 mantissa, i.e. 1.xxx Q30

      /* use table look-up of man in [1.0, 2.0[ Q30 */
      a0 = L_shr(L_sub(a0, (Word32)0x40000000), 8);      // Q16 index into table - note zero'ing of leading 1
      idx_man = extract_h(a0);                           // Q0 index into table
      sub_man = extract_l(L_shr((a0 & 0xFFFF), 1));      // Q15 fractional sub_man
      a0 = L_deposit_h(tablog[idx_man]);                 // Q31
      sub_tab = sub(tablog[idx_man+1], tablog[idx_man]); // Q15
      a0 = L_mac(a0, sub_man, sub_tab);                  // Q31

      *frac_comp = intround(a0);                             // Q15
      *int_comp = sub(30, exp);                          // Q0
   }

   return;
}

/*******************************************/
/* y = sqrt(x)                             */
/* table look-up with linear interpolation */
/*******************************************/

Word16 sqrts(Word16 x)
{
   Word16 xb, y, exp, idx, sub_frac, sub_tab;
   Word32 a0;

   if(x <= 0){
      y = 0;
   }
   else{
      exp = norm_s(x);

      /* use 65-entry table */
      xb = shl(x, exp);                            // normalization of x
      idx = shr(xb, 9);                            // for 65 entry table
      a0 = L_deposit_h(tabsqrt[idx]);              // Q31 table look-up value
      sub_frac = shl((Word16)(xb & 0x01FF), 6);    // Q15 sub-fraction
      sub_tab = sub(tabsqrt[idx+1], tabsqrt[idx]); // Q15 table interval for interpolation
      a0 = L_mac(a0, sub_frac, sub_tab);           // Q31 linear interpolation between table entries
      if(exp & 0x0001){
         exp = shr(add(exp, 1), 1);                // normalization of sqrt()
         a0 = L_shr(a0, exp);
         y = intround(a0);                            // Q15
         a0 = L_mac(a0, 13573, y);                 // Q31 incorporate the missing "/sqrt(2)"
      }
      else{
         exp = shr(exp, 1);                        // normalization of sqrt()
         a0 = L_shr(a0, exp);                      // Q31
      }
      y = intround(a0);                               // Q15
   }

   return y;
}

/****************************************************/
/* y = 1/sqrt(x)                                    */
/* table look-up for sqrt with linear interpolation */
/* use div_s for inverse                            */
/****************************************************/

void sqrt_i(Word16 x_man, Word16 x_exp, Word16 *y_man, Word16 *y_exp)
{
   Word16 x_manb, x_expb, y, exp, idx, sub_frac, sub_tab;
   Word32 a0;

   if(x_man <= 0){
      *y_man = 0;
      *y_exp = 0;
   }
   else{

      exp = norm_s(x_man);
      x_manb = shl(x_man, exp);                    // normalize to Q15 in [0; 1] for table look-up
      x_expb = add(x_exp, exp);                    // update exponent for x (left-shifting by additionl exp)
      x_expb = sub(x_expb, 15);                    // we need to take sqrt of 0-32767 number but table is for 0-1
      idx = shr(x_manb, 9);                        // for 65 entry table
      a0 = L_deposit_h(tabsqrt[idx]);              // Q31 table look-up value
      sub_frac = shl((Word16)(x_manb & 0x01FF), 6);// Q15 sub-fraction
      sub_tab = sub(tabsqrt[idx+1], tabsqrt[idx]); // Q15 table interval for interpolation
      a0 = L_mac(a0, sub_frac, sub_tab);           // Q31 linear interpolation between table entries
      exp = norm_l(a0);                            // exponent of a0
      y = intround(L_shl(a0,exp));                    // normalize sqrt-root and drop 16 LBSs
      exp = add(15, exp);                          // exponent of a0 taking Q15 of y into account

      if(x_expb & 0x0001){
         if(y < 0x5A82){                           // 1*sqrt(2) in Q14) - with div_s(y1, y2), y2 must be >= y1
            exp = add(exp, shr(add(x_expb,1), 1)); // normalization for sqrt()
            *y_man = div_s(0x2D41, y);             // 0x2D41 is 1/sqrt(2) in Q14 
         }
         else{
            exp = add(exp, shr(sub(x_expb,1), 1)); // normalization for sqrt()
            *y_man = div_s(0x5A82, y);             // 0x5A82 is 1*sqrt(2) in Q14 
         }
         *y_exp = sub(29, exp);                    // ...and div_s returns fraction divide in Q15
      }
      else{
         exp = add(exp, shr(x_expb, 1));           // normalization for sqrt()
         *y_man = div_s(0x4000, y);
         *y_exp = sub(29, exp);                    // 0x4000 is 1 in Q14 and div_s returns fraction divide in Q15
      }
   }

   return;
}
