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
  bv16cnst.h : BV16 constants

  $Log$
******************************************************************************/

#ifndef  BV16CNST_H
#define  BV16CNST_H

/* ----- Basic Codec Parameters ----- */
#define FRSZ 	40 		/* frame size */
#define WINSZ	160		/* lpc analysis WINdow SiZe  */
#define MAXPP  137      /* MAXimum Pitch Period         		  */
#define MINPP  10       /* MINimum Pitch Period          		  */
#define PWSZ	120	   /* Pitch analysis Window SiZe   */
#define MAXPP1 (MAXPP+1)/* MAXimum Pitch Period + 1  	  */

/* Quantization parameters */
#define VDIM		  4	/* excitation vector dimension */		
#define CBSZ		 16	/* excitation codebook size */
#define PPCBSZ     32	/* pitch predictor codebook size */
#define LGPORDER	  8	/* Log-Gain Predictor OODER */
#define LGPECBSZ	 16	/* Log-Gain Prediction Error CodeBook SiZe */
#define LSPPORDER   8	/* LSP MA Predictor ORDER */
#define LSPECBSZ1 128	/* codebook size of 1st-stage LSP VQ */
#define LSPECBSZ2  64	/* codebook size of 2nd-stage LSP VQ; 1-bit for sign */

/* Excitation gain quantization */
#define GPO       8     /* order of MA prediction       */
#define NG       18     /* number of relative gain bins */
#define GLB     -24.    /* lower relative gain bound    */
#define NGC      12     /* number of gain change bins   */
#define GCLB     -8.    /* lower gain change bound      */
#define Minlg     0.    /* minimum log-gain             */
#define TMinlg    1.    /* minimum linear gain          */
#define LGCBSZ   16     /* size of codebook             */

/* Definitions for periodicity to gain scaling mapping */
#define ScPLCGmin   1639 /* 0.1 Q14 */
#define ScPLCGmax  14746 /* 0.9 Q14 */
#define ScPLCG_b  -32768 /* -2.0 in Q14 */
#define ScPLCG_a   31129 /* 1.9 in Q14 */
#define HoldPLCG  8
#define AttnPLCG 50
#define AttnFacPLCG 20971

/* Level Estimation */
#define estl_alpha  32760
#define estl_alpha1 32640
#define estl_beta   32704
#define estl_beta1     64
#define estl_a      32640
#define estl_a1       128
#define estl_TH      6554
#define Nfdm          100 /* Max number of frames with fast decay of Lmin */

/* Log-Gain Limitation */
#define LGLB  -24 /* Log-Gain Lower Bound */
#define LGCLB -8  /* Log-Gain Change Lower Bound */
#define NGB    18 /* Number of Gain Bins */
#define NGCB   12 /* Number of Gain Change Bins */

/* Buffer offsets and sizes */
#define XOFF    MAXPP1       /* offset for x() frame      */
#define LX      (XOFF+FRSZ)  /* Length of x() buffer      */
#define XQOFF   (MAXPP1)     /* xq() offset before current subframe */
#define LXQ     (XQOFF+FRSZ) /* Length of xq() buffer */
#define LTMOFF	 (MAXPP1)	  /* Long-Term filter Memory OFFset */

/* Long-term postfilter */
#define DPPQNS     4 /* Delta pitch period for search             */
#define NINT      20 /* length of filter interpolation            */
#define ATHLD1 18022 /* 0.55 Q15 threshold on normalized pitch correlation */
#define ATHLD2 26214 /* 0.80 Q15 threshold on normalized pitch correlation */
#define ScLTPF  9830 /* 0.3 Q15 scaling of LTPF coefficient               */

/* Coarse pitch search */
#define  MAX_NPEAKS  7
#define TH1   23921 /* first threshold for cor*cor/energy   */
#define TH2   13107 /* second threshold for cor*cor/energy  */
#define LPTH1 25887 /* Last Pitch cor*cor/energy THreshold 1 */
#define LPTH2 14090 /* Last Pitch cor*cor/energy THreshold 2 */
#define MPDTH  2130 /* Multiple Pitch Deviation THreshold */
#define SMDTH  3113 /* Sub-Multiple pitch Deviation THreshold  0.125 */
#define MPTH4  9830

/* Decimation parameters */
#define DECF    4                 /* DECimation Factor for coarse pitch period search   */
#define FRSZD   (FRSZ/DECF)       /* FRame SiZe in DECF:1 lowband domain    */
#define MAXPPD  (MAXPP/DECF)      /* MAX Pitch in DECF:1, */
#define MINPPD  ((int)(MINPP/DECF)) /* MINimum Pitch Period in DECF:1 */
#define PWSZD   (PWSZ/DECF)       /* Pitch ana Window SiZe in DECF:1 domain */
#define DFO     4
#define MAXPPD1 (MAXPPD+1)
#define LXD     (MAXPPD1+PWSZD)
#define XDOFF   (LXD-FRSZD)
#define HMAXPPD (MAXPPD/2)
#define M1      (MINPPD-1)
#define M2      MAXPPD1
#define HDECF   (DECF/2)

/* Front-end 150 Hz highpass filter */ 
#define HPO 2 /* High-pass filter order */

/* LPC weighting filter */
#define LTWF 4096 /* 0.5 in Q13 perceptual Weighting Factor Lowband  */

/* pole-zero NFC shaping filter */
#define NSTORDER 8

#endif /* BV16CNST_H */
