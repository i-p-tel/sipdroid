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
  stblzlsp.c : Common Fixed-Point Library: stabilize the lsp's

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "basop32.h"

void stblz_lsp(
               Word16   *lsp,       /* Q15 */
               Word16   order)
{
   
   /* This function orders the lsp to prevent      */
   /* unstable synthesis filters and imposes basic */ 
   /* lsp properties in order to avoid marginal    */
   /* stability of the synthesis filter.           */
   
   Word16 k, i;
   Word16 mintmp, maxtmp, a0;
   
   /* order lsps as minimum stability requirement */
   do {
      k = 0;              /* use k as a flag for order reversal */
      for (i = 0; i < order - 1; i++) {
         if (lsp[i] > lsp[i+1]) { /* if there is an order reversal */
            a0       = lsp[i+1];         
            lsp[i+1] = lsp[i];    /* swap the two LSP elements */
            lsp[i]   = a0;
            k        = 1;      /* set the flag for order reversal */
         }
      }
   } while (k > 0); /* repeat order checking if there was order reversal */
   
   
   /* impose basic lsp properties */
   maxtmp=sub(LSPMAX,(Word16)((order-1)*DLSPMIN));
   
   if(lsp[0] < LSPMIN) lsp[0] = LSPMIN;
   else if(lsp[0] > maxtmp) lsp[0] = maxtmp;
   
   for(i=0; i<order-1; i++){
      /* space lsp(i+1) */
      
      /* calculate lower and upper bound for lsp(i+1) */
      mintmp = add(lsp[i],DLSPMIN);
      maxtmp = add(maxtmp, DLSPMIN);
      
      /* guarantee minimum spacing to lsp(i) */
      if(lsp[i+1] < mintmp) lsp[i+1] = mintmp;
      
      /* make sure the remaining lsps fit within the remaining space */
      else if(lsp[i+1] > maxtmp) lsp[i+1] = maxtmp;
      
   }
   
   return;
}

Word16 stblchck(
                Word16 *x,
                Word16 vdim)
{
   Word16 k, stbl;
   
   if (x[0] < 0) stbl = 0;
   else 
   {
      stbl = 1;
      for (k=1; k<vdim; k++) 
         if (x[k] < x[k-1]) 
            stbl = 0;
   }
   return stbl;
}
