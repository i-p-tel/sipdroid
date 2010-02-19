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
  preproc.c : Preprocssing (input highpass filter)

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"

/************************************************************************/
/* 2nd order pole-zero 150Hz high-pass filter designed with Matlab:     */
/* [b a]=ellip(2,.25,40,150/4000,'high');                               */
/* Note that down-scaling by a factor 1.5 is built into b-coefficients  */
/*                                                                      */
/* y[n]=b[0]*x[n]+b[1]*x[n-1]+b[2]*x[n-2]-a[1]*y[n-1]-a[2]*y[n-2]       */
/************************************************************************/

void preprocess(
                struct BV16_Encoder_State *cs,
                Word16 *output,                 /* (o) Q0 output signal, less factor 1.5  */
                Word16 *input,                  /* (i) Q0 input signal                    */
                Word16 N)                       /* length of signal                       */
{
   Word16 n;
   Word32 a0;
   
   for(n=0; n<N; n++)
   {
      
      /* pole section of filtering */
      a0 = Mpy_32_16(cs->hpfpm[0], cs->hpfpm[1], hpfa[1]);              // Q14
      a0 = L_add(a0, Mpy_32_16(cs->hpfpm[2], cs->hpfpm[3], hpfa[2]));   // Q14

      /* zero section of filtering */
      a0 = L_mac0(a0, input[n], hpfb[0]);                               // Q14
      a0 = L_mac0(a0, cs->hpfzm[0], hpfb[1]);                           // Q14
      a0 = L_mac0(a0, cs->hpfzm[1], hpfb[2]);                           // Q14
      a0 = L_shl(a0, 1);                                                // Q15

      /* update pole section of memory */
      cs->hpfpm[2] = cs->hpfpm[0];
      cs->hpfpm[3] = cs->hpfpm[1];
      L_Extract(a0, cs->hpfpm, cs->hpfpm+1);

      /* get output in Q0 (less down-scaling by factor 1.5) */
      a0 = L_shl(a0, 1);                                                // Q16
      output[n] = intround(a0);                                            // Q0

      /* update zero section of memory */
      cs->hpfzm[1] = cs->hpfzm[0];                                      // Q0
      cs->hpfzm[0] = input[n];                                          // Q0
   }
}
