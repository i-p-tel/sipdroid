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
  allzero.c : Common Fixed-Point Library: all-zero filter

  $Log$
******************************************************************************/

#include "typedef.h"
#include "basop32.h"

void azfilter(
              Word16 a[],    /* (i) Q12 : prediction coefficients          */
              Word16 m,      /* (i)     : LPC order                        */
              Word16 x[],    /* (i) Q0  : input signal samples, incl. past */
              Word16 y[],    /* (o) Q0  : filtered output signal           */
              Word16 lg      /* (i)     : size of filtering                */
              )
{
   Word16 i, n;
   Word32 a0;
   Word16 *fp1;

   /* loop through every element of the current vector */
   for (n = 0; n < lg; n++) {
      
      /* perform multiply-adds along the delay line of filter */
      fp1 = x + n;
      a0 = L_mult0(a[0], *fp1--); // Q12
      for (i = 1; i <= m; i++)
         a0 = L_mac0(a0, a[i], *fp1--); // Q12
      
      /* get the output with rounding */
      y[n] = intround(L_shl(a0, 4)); // Q0
   }

   return;
}
