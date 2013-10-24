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
  allpole.c : Common Fixed-Point Library: all-pole filter

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "basop32.h"
#include "utility.h"

#define BUFFERSIZE  (LPCO+160)

void apfilter(
              Word16 a[],     /* (i) Q12 : prediction coefficients  */
              Word16 m,       /* (i)     : LPC order                */
              Word16 x[],     /* (i) Q0  : input signal             */
              Word16 y[],     /* (o) Q0  : output signal            */
              Word16 lg,      /* (i)     : size of filtering        */
              Word16 mem[],   /* (i/o) Q0: filter memory            */
              Word16 update   /* (i)     : memory update flag       */
              )
{
   Word16 buf[BUFFERSIZE]; /* buffer for filter memory & signal */
   Word32 a0;
   Word16 *fp1;
   Word16 i, n;
   
   /* copy filter memory to beginning part of temporary buffer */
   W16copy(buf, mem, m);
   
   /* loop through every element of the current vector */
   for (n = 0; n < lg; n++) {
      
      /* perform multiply-adds along the delay line of filter */
      fp1 = &buf[n];
      a0 = L_mult0(4096, x[n]); // Q12
      for (i = m; i > 0; i--)
         a0 = L_msu0(a0, a[i], *fp1++); // Q12
      
      /* update temporary buffer for filter memory */
      *fp1 = intround(L_shl(a0,4));
   }

   /* copy to output array */
   W16copy(y, buf+m, lg);
   
   /* get the filter memory after filtering the current vector */
   if(update)
      W16copy(mem, buf+lg, m);

   return;
}
