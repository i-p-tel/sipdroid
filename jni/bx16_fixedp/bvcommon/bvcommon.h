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
  bvcommon.h : Common Fixed-Point Library: common #defines and prototypes

  $Log$
******************************************************************************/

#ifndef  BVCOMMON_H
#define  BVCOMMON_H

/* ----- Basic Codec Parameters ----- */

#define  LPCO  8        /* LPC Order for 8 kHz sampled lowband signal     */
#define  Ngrd     60

#define  LSPMIN  49      /* 0.00150 minimum lsp frequency */
#define  LSPMAX  32694   /* 0.99775 maximum lsp frequency */
#define  DLSPMIN 410     /* 0.01250 minimum lsp spacing */
#define  STBLDIM 3          /* dimension of stability enforcement */

/* LPC bandwidth expansion */
extern   Word16   bwel[];

/* LPC to lsp Conversion */
extern   Word16   grid[];

/* LPC WEIGHTING FILTER */
extern   Word16   STWAL[];

/* Coarse Pitch Search */
extern   Word16   invk[];

/* Pitch tap codebook - actually content different for BV16 and BV32 */
extern Word16 pp9cb[];

/* Function prototypes */

void azfilter(
              Word16 a[],    /* (i) Q12 : prediction coefficients          */
              Word16 m,      /* (i)     : LPC order                        */
              Word16 x[],    /* (i) Q0  : input signal samples, incl. past */
              Word16 y[],    /* (o) Q0  : filtered output signal           */
              Word16 lg      /* (i)     : size of filtering                */
              );

void apfilter(
              Word16 a[],     /* (i) Q12 : prediction coefficients  */
              Word16 m,       /* (i)     : LPC order                */
              Word16 x[],     /* (i) Q0  : input signal             */
              Word16 y[],     /* (o) Q0  : output signal            */
              Word16 lg,      /* (i)     : size of filtering        */
              Word16 mem[],   /* (i/o) Q0: filter memory            */
              Word16 update   /* (i)     : memory update flag       */
              );

void lsp2a(
Word16 lsp[],    /* (i) Q15 : line spectral frequencies            */
Word16 a[]);     /* (o) Q12 : predictor coefficients (order = 10)  */

void stblz_lsp(
Word16  *lsp,       /* Q15 */
Word16  order);

Word16 stblchck(
Word16 *x,
Word16 vdim);

void a2lsp(
Word16 a[],        /* (i) Q12 : predictor coefficients              */
Word16 lsp[],      /* (o) Q15 : line spectral frequencies           */
Word16 old_lsp[]); /* (i)     : old lsp[] (in case not found 10 roots) */

void Autocorr(
              Word32   r[],     /* (o) : Autocorrelations      */
              Word16   x[],     /* (i) : Input signal          */
              Word16   window[],/* (i) : LPC Analysis window   */ 
              Word16   l_window,/* (i) : window length         */
              Word16   m);      /* (i) : LPC order             */    

void Spectral_Smoothing(
Word16 m,         /* (i)     : LPC order                    */
Word32 rl[],      /* (i/o)   : Autocorrelations  lags       */
Word16 lag_h[],   /* (i)     : SST coefficients  (msb)      */
Word16 lag_l[]);  /* (i)     : SST coefficients  (lsb)   */  

void Levinson(
  Word32 r32[],    /* (i)  : r32[] double precision vector of autocorrelation coefficients   */
  Word16 a[],      /* (o)  : a[] in Q12 - LPC coefficients                                   */
  Word16 old_a[],  /* (i/o): old_a[] in Q12 - previous LPC coefficients                      */
  Word16 m);	    /* (i)  : LPC order                                                       */

void pp3dec(
Word16  idx,
Word16  *b);

void vqdec(
Word16  *xq,
Word16  idx,
Word16  *cb,
Word16  vdim);


#endif /* BVCOMMON_H */
