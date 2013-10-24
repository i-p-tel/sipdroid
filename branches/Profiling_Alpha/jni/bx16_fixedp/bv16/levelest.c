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
  levelest.c : Input level estimation

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bv16cnst.h"
#include "basop32.h"

Word32   estlevel(
                  Word32   lg,
                  Word32   *level,
                  Word32   *lmax,
                  Word32   *lmin,
                  Word32   *lmean,
                  Word32   *x1,
                  Word16   ngfae,
                  Word16   nggalgc,
                  Word16   *estl_alpha_min)
{
   Word32   lth;
   Word32   a0;
   Word16   s, t;
   
   /* Reset forgetting factor for Lmin to fast decay. This is to avoid Lmin
   staying at an incorrect low level compensation for the possibility 
   it has caused incorrect bit-error declaration by making 
   the estimated level too low. */
   
   if(nggalgc == 0) *estl_alpha_min = estl_alpha1;
   
   /* Reset forgetting factor for Lmin to regular decay if fast decay
   has taken place for the past Nfdm frames. */
   
   else if (nggalgc == Nfdm+1) *estl_alpha_min = estl_alpha;   
   
   /* Update the new maximum, minimum, & mean of log-gain */
   if (lg > *lmax) *lmax=lg;  /* use new log-gain as max if it is > max */
   else {                     /* o.w. attenuate toward lmean */
      
      /* *lmax=*lmean+estl_alpha*(*lmax-*lmean); */
      a0 = L_sub(*lmax, *lmean);
      L_Extract(a0, &s, &t);
      a0 = Mpy_32_16(s, t, estl_alpha);
      *lmax = L_add(a0, *lmean);
   }
   
   if (lg < *lmin && ngfae == LGPORDER+1 && nggalgc > LGPORDER) {
      *lmin = lg; /* use new log-gain as min if it is < min */
                  *estl_alpha_min = estl_alpha; /* Reset forgetting factor for Lmin to 
                                                regular decay in case it has been on
                                                fast decay since it has now found 
                  a new minimum level. */
   }
   else {               /* o.w. attenuate toward lmean */
      
      /* *lmin=*lmean+(*estl_alpha_min)*(*lmin-*lmean); */
      a0 = L_sub(*lmin, *lmean);
      L_Extract(a0, &s, &t);
      a0 = Mpy_32_16(s, t, (*estl_alpha_min));
      *lmin = L_add(a0, *lmean);
   }
   
   /* *lmean=estl_beta*(*lmean)+estl_beta1*(0.5*(*lmax+*lmin)); */
   a0 = L_shr(L_add(*lmax, *lmin),1);
   L_Extract(a0, &s, &t);
   a0 = Mpy_32_16(s, t, estl_beta1);
   L_Extract(*lmean, &s, &t);
   *lmean = L_add(a0, Mpy_32_16(s, t, estl_beta));
   
   /* update estimated input level, by calculating a running average
   (using an exponential window) of log-gains exceeding lmean */
   /* lth=*lmean+estl_TH*(*lmax-*lmean); */
   
   a0 = L_sub(*lmax, *lmean);
   L_Extract(a0, &s, &t);
   lth = L_add(*lmean, Mpy_32_16(s, t, estl_TH));
   
   if (lg > lth) {
      
      /* *x1=estl_a*(*x1)+estl_a1*lg; */
      L_Extract(*x1, &s, &t);
      a0 = Mpy_32_16(s, t, estl_a);
      L_Extract(lg, &s, &t);
      *x1 = L_add(a0, Mpy_32_16(s, t, estl_a1));
      
      /* *level=estl_a*(*level)+estl_a1*(*x1); */
      L_Extract(*level, &s, &t);
      a0 = Mpy_32_16(s, t, estl_a);
      L_Extract(*x1, &s, &t);
      *level = L_add(a0, Mpy_32_16(s, t, estl_a1));  
   }
   return   lth;   
}
