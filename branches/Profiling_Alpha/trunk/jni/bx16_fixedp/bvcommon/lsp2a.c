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
  lsp2a.c : Common Fixed-Point Library: conversion from lsp's to a's

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "basop32.h"
#include "mathutil.h"

void get_pq_polynomials(
                        Word32 *f,     /* Q23 */
                        Word16 *lsp);  /* Q15 */

void lsp2a(
           Word16 lsp[],    /* (i) Q15 : line spectral pairs                  */
           Word16 a[])      /* (o) Q12 : predictor coefficients (order = 10)  */
{
   Word32 p[LPCO+1], q[LPCO+1]; // Q23
   Word32 a0;
   Word16 i, n;

   get_pq_polynomials(p, lsp);
   get_pq_polynomials(q, lsp+1);

   a[0] = 4096;                  // Q12
   a0 = L_add(p[1], q[1]);       // Q23
   a[1] = intround(L_shl(a0,4));    // Q12 - includes 0.5 factor of a[1] = 0.5*(p[1]+q[1])
   for(i=1, n=2; i<LPCO; i++, n++){
      a0 = L_add(p[i], p[n]);    // Q23
      a0 = L_add(a0, q[n]);      // Q23
      a0 = L_sub(a0, q[i]);      // Q23
      a[n] = intround(L_shl(a0,4)); // Q12 a[n] = 0.5 * (p[i] + p[n] + q[n] - q[i]);
   }

   return;
}

void get_pq_polynomials(
                        Word32 *f,     /* Q23 */
                        Word16 *lsp)   /* Q15 */
{
   Word16 i, n, hi, lo;
   Word16 index, offset, coslsp, c;
   Word32 a0;

   f[0] = L_mult(2048, 2048);                                        // 1.0 Q23
   for(i = 1; i <= LPCO ; i++)
      f[i]= 0;

   for(n=1; n<=(LPCO>>1); n++) { 

      /* cosine mapping */
      index = shr(lsp[2*n-2],9);                                     // Q6
      offset = lsp[2*n-2]&(Word16)0x01ff;                            // Q9
      a0 = L_mult(sub(costable[index+1], costable[index]), offset);  // Q10
      coslsp = add(costable[index], intround(L_shl(a0, 6)));            // Q15 cos((double)PI*lsp[2*n-2])

      c = coslsp;                                                    // Q14 c = 2. * cos((double)PI*lsp[2*n-2])

      for(i = 2*n; i >= 2; i--){
         L_Extract(f[i-1], &hi, &lo);

         f[i] = L_add(f[i], f[i-2]);                                 // Q23 f[i] += f[i-2]
         a0 = Mpy_32_16(hi, lo, c);                                  // Q22
         f[i] = L_sub(f[i], L_shl(a0,1));                            // Q23 f[i] += f[i-2] - c*f[i-1];
      }
      f[1] = L_msu(f[1], c, 256);                                    // Q23 f[1] -= c;
   }

   return;
}
