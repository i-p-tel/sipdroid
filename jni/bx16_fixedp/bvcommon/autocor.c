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
  autocor.c : Common Fixed-Point Library: window an input array and 
              compute autocorrelation coefficients

  $Log$
******************************************************************************/

#include "typedef.h"
#include "basop32.h"

#define  WINSZ 160

void Autocorr(
              Word32   r[],     /* (o) : Autocorrelations      */
              Word16   x[],     /* (i) : Input signal          */
              Word16   window[],/* (i) : LPC Analysis window   */ 
              Word16   l_window,/* (i) : window length         */
              Word16   m)       /* (i) : LPC order             */    
{
   Word16 n, j, y_shift, shift;
   Word16 buf[WINSZ];
   Word32 a0;

   /* Window signal */
   for(n=0; n<l_window; n++)
      buf[n] = mult_r(x[n], window[n]);

   /* First find shifting to prevent overflow */
   a0 = 1;
   for(n=0; n<l_window; n++){
      y_shift = shr(buf[n], 4);  // guarantees no overflow in sum of squares (l_window is 160)
      a0 = L_mac0(a0, y_shift, y_shift);
   }

   /* Calculate necessary shift */
   shift = sub(4, shr(norm_l(a0),1));
   if(shift < 0)
      shift = 0;

   /* Shift input and calculate autocorrelation coefficient r[0] */
   a0 = 1;
   for(n=0; n<l_window; n++){ 
      buf[n] = shr(buf[n], shift);
      a0 = L_mac0(a0, buf[n], buf[n]);
   }

   /* Normalize autocorrelation */
   shift = norm_l(a0);
   r[0] = L_shl(a0, shift);

   /* Calculate autocorrelation coefficients r[1], r[2], ..., r[m] */
   for(j=1; j<=m; j++){
      a0 = 0;
      for(n=0; n<l_window-j; n++)
         a0 = L_mac0(a0, buf[n], buf[n+j]);
      r[j] = L_shl(a0, shift);
   }

   return;
}

void Spectral_Smoothing(
                        Word16 m,         /* (i)     : LPC order         */
                        Word32 r[],       /* (i/o)   : Autocorrelations  */
                        Word16 lag_h[],   /* (i)     : SST coefficients  */
                        Word16 lag_l[]    /* (i)     : SST coefficients  */  
                        )
{
   Word16    i;
   Word16 hi, lo;
   
   for(i=1; i<=m; i++)
   {
      L_Extract(r[i], &hi, &lo);
      r[i] = Mpy_32(hi, lo, lag_h[i-1], lag_l[i-1]);
   }
}
