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
  postfilt.c : Pitch postfilter.

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "basop32.h"
#include "mathutil.h"

/* Standard Long-Term Postfilter */
void postfilter(
                Word16 *s,   /* input : quantized speech signal         */
                Word16 pp,   /* input : pitch period                    */
                Word16 *ma_a,
                Word16 *b_prv,
                Word16 *pp_prv,
                Word16 *e)   /* output: enhanced speech signal          */
{
   int n;
   Word16 len, t0, t1, t2, t3, shift, aa, R0norm, R0_exp;
   Word32 a0, a1, R0, R1, R01, R01max, Rx;
   Word16 *fp1;
   Word16 ppt, pptmin, pptmax, ppnew;
   Word16 bb[2];
   Word16 R1max_exp, R1max, R01Sqmax_exp, R01Sqmax, R01Sq_exp, R01Sq, R1_exp, R1n;
   Word16 gainn, Rx_exp;
   Word16 buf[MAXPP+FRSZ];
   Word16 *ps, ww1, ww2;
   Word32 step, delta;
   Word16 bi0, bi1c, bi1p;
   
   ps = s+XQOFF;
   
   /********************************************************************/
   /*                 pitch search around decoded pitch                */
   /********************************************************************/
   pptmin = sub(pp, DPPQNS);
   pptmax = add(pp, DPPQNS);
   if (pptmin<MINPP)
   {
      pptmin = MINPP;
      pptmax = add(pptmin, 2*DPPQNS);
   }
   else if (pptmax>MAXPP)
   {
      pptmax = MAXPP;
      pptmin = sub(pptmax, 2*DPPQNS);
   }
   
   fp1 = &s[XQOFF-pptmax];
   len = add(FRSZ, pptmax);
   a0 = 0;
   for (n=0;n<len;n++) 
   {
      t1 = shr(*fp1++, 3);
      a0 = L_mac0(a0,t1,t1);
   }
   shift = norm_l(a0);
   if (a0==0) shift=31;
   shift = sub(6, shift);
   if (shift > 0)
   {
      ps = buf+pptmax;
      fp1 = &s[XQOFF-pptmax];
      shift = shr(add(shift, 1), 1);
      for (n=0;n<len;n++)
      {
         buf[n] = shr(fp1[n], shift);
      }
   }
   else shift=0;
   
   R0  = 0;
   R1  = 0;
   R01 = 0;
   for(n=0; n<FRSZ; n++)
   {
      R0  = L_mac0(R0, ps[n], ps[n]);
      R1  = L_mac0(R1, ps[n-pptmin], ps[n-pptmin]);
      R01 = L_mac0(R01,ps[n], ps[n-pptmin]);
   }
   R0_exp = norm_l(R0);
   R0norm = extract_h(L_shl(R0, R0_exp));
   R0_exp = R0_exp-16;
   
   ppnew        = pptmin;
   R1max_exp    = norm_l(R1);
   R1max        = extract_h(L_shl(R1, R1max_exp));
   R01Sqmax_exp = norm_l(R01);
   t1           = extract_h(L_shl(R01, R01Sqmax_exp));
   R01Sqmax_exp = shl(R01Sqmax_exp, 1);
   R01Sqmax     = extract_h(L_mult(t1, t1));
   R01max       = R01;
   for(ppt=pptmin+1; ppt<=pptmax; ppt++)
   {
      R1 = L_msu0(R1,ps[FRSZ-ppt], ps[FRSZ-ppt]);
      R1 = L_mac0(R1,ps[-ppt], ps[-ppt]);      
      R01= 0;
      for(n=0; n<FRSZ; n++)
      {
         R01 = L_mac0(R01, ps[n], ps[n-ppt]);
      }
      R01Sq_exp = norm_l(R01);
      t1 = extract_h(L_shl(R01, R01Sq_exp));
      R01Sq_exp = shl(R01Sq_exp, 1);
      R01Sq = extract_h(L_mult(t1, t1));
      R1_exp = norm_l(R1);
      R1n = extract_h(L_shl(R1, R1_exp));
      
      a0 = L_mult(R01Sq, R1max);
      a1 = L_mult(R01Sqmax, R1n);
      t1 = add(R01Sq_exp, R1max_exp);
      t2 = add(R01Sqmax_exp, R1_exp);
      
      t2 = sub(t1, t2);
      if (t2>=0) a0 = L_shr(a0, t2);
      if (t2<0)  a1 = L_shl(a1, t2); 
      
      if (L_sub(a0, a1)>0) 
      {
         R01Sqmax = R01Sq; 
         R01Sqmax_exp = R01Sq_exp;
         R1max = R1n; R1max_exp = R1_exp;
         ppnew = ppt;
         R01max = R01;
      }
   }
   
   /******************************************************************/
   /*               calculate all-zero pitch postfilter              */
   /******************************************************************/
   if (R1max==0 || R0==0 || R01max <= 0)
   {
      aa = 0;
   }
   else
   {
      a0 = R1max_exp-16;
      t1 = mult(R1max, R0norm);
      a0 = a0+R0_exp-15;
      sqrt_i(t1, (Word16)a0, &t1, &t2);
      t0 = norm_l(R01max);
      t3 = extract_h(L_shl(R01max, t0));
      t0 = t0-16;
      aa = mult(t3, t1);
      t0 = t0+t2-15;
      t0 = t0-15;
      if (t0<0) aa = shl(aa, sub(0,t0));
      else aa = shr(aa, t0);
   }
   a0 = L_mult(8192, aa);
   a0 = L_mac(a0, 24576, *ma_a);
   *ma_a = intround(a0);
   if((*ma_a < ATHLD1) && (aa < (ATHLD2)))
      aa = 0;
   bb[1] = mult(ScLTPF, aa);
   
   /******************************************************************/
   /*             calculate normalization energies                   */
   /******************************************************************/
   Rx = 0;
   R0 = 0;
   for(n=0; n<FRSZ; n++)
   {
      a0   = L_shl(s[XQOFF+n], 15);
      a0   = L_add(a0, L_mult0(bb[1], s[XQOFF+n-ppnew]));
      e[n] = intround(a0);
      t1   = shr(e[n], shift);
      t2   = shr(s[XQOFF+n], shift);
      Rx   = L_mac0(Rx, t1, t1);
      R0   = L_mac0(R0, t2, t2);
   }
   R0 = L_shr(R0, 2);
   if(R0 == 0 || Rx == 0)
      gainn = 32767;
   else
   {
      Rx_exp = norm_l(Rx);
      t1 = extract_h(L_shl(Rx, Rx_exp));
      t2 = extract_h(L_shl(R0, Rx_exp));
      if (t2>= t1)
         gainn = 32767;
      else
      {
         t1 = div_s(t2, t1);
         gainn = sqrts(t1);
      }
   }
   
   /******************************************************************/
   /*    interpolate from the previous postfilter to the current     */
   /******************************************************************/
   bb[0] = gainn;
   bb[1] = mult(gainn, bb[1]);
   step  = (Word32)((1.0/(NINT+1))*(2147483648.0));
   delta = 0;
   for(n=0; n<NINT; n++)
   {
      delta = L_add(delta, step);
      ww1   = intround(delta);
      ww2   = add(sub(32767, ww1), 1);
      /* interpolate between two filters */
      bi0 = intround(L_mac(L_mult(ww1, bb[0]), ww2, b_prv[0]));
      bi1c= mult(ww1, bb[1]);
      bi1p= mult(ww2, b_prv[1]);
      e[n] = intround(L_mac(L_mac(L_mult(bi1c, s[XQOFF+n-ppnew]), bi1p, s[XQOFF+n-(*pp_prv)]), bi0, s[XQOFF+n]));
   }
   for(n=NINT; n<FRSZ; n++)
   {
      e[n] = intround(L_shl(L_mult(gainn, e[n]),1));
   }
   
   /******************************************************************/
   /*                       save state memory                        */
   /******************************************************************/
   *pp_prv = ppnew;
   b_prv[0] = bb[0];
   b_prv[1] = bb[1];
   
   return;
}

