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
  bv16externs.c : BV16 Fixed-Point externs

  $Log$
******************************************************************************/

/* POINTERS */
extern   Word16  winl[WINSZ];
extern   Word16  sstwinl_h[];
extern   Word16  sstwinl_l[];
extern   Word16  gfsz[];
extern   Word16  gfsp[];
extern   Word16  idxord[];
extern   Word16  hpfa[];
extern   Word16  hpfb[];
extern   Word16  adf_h[];
extern   Word16  adf_l[];
extern   Word16  bdf[];
extern   Word16  x[];
extern   Word16  x2[];
extern   Word16  MPTH[];

/* LSP Quantization */
extern   Word16  lspecb1[];
extern   Word16  lspecb2[];
extern   Word16  lspmean[];
extern   Word16  lspp[];

/* Log-Gain Quantization */
extern   Word16  lgpecb[];
extern   Word16  lgp[];
extern   Word16  lgmean;

/* Log-Gain Limitation */
extern   Word16  lgclimit[];

/* Excitation Codebook */
extern   Word16  cccb[];

/* Function Prototypes */
Word32  estlevel(
Word32  lg,
Word32  *level,
Word32  *lmax,
Word32  *lmin,
Word32  *lmean,
Word32  *x1,
Word16   ngfae,
Word16   nggalgc,
Word16   *estl_alpha_min); /* Q15 */

void excdec_w_synth(
Word16 *xq,      /* (o) Q0 quantized signal vector               */
Word16 *ltsym,   /* (i/o) Q16 quantized excitation signal vector */
Word16 *stsym,   /* (i/o) Q0 short-term predictor memory         */
Word16 *idx,     /* (o) quantizer codebook index for uq[] vector */
Word16 *b,       /* (i) Q15 coefficient of 3-tap pitch predictor */
Word16 *cb,      /* (i) Q0 codebook                              */
Word16 pp,       /* pitch period (# of 8 kHz samples)            */
Word16 *aq,      /* (i) Q12 short-term predictor coefficients    */
Word16 gain_exp, /* gain_exp of current sub-frame                */
Word32 *EE
);

Word32 gaindec(
Word32   *lgq,    /* Q25 */
Word16  gidx,
Word16  *lgpm,      /* Q11 */
Word32  *prevlg,  /* Q25 */
Word32   level,      /* Q25 */
Word16   *nggalgc,
Word32   *lg_el);

void gainplc(Word32 E, Word16 *lgeqm, Word32 *lgqm);

void lspdec(
Word16  *lspq,       /* Q15 */ 
Word16  *lspidx,  
Word16  *lsppm,      /* Q15 */
Word16  *lspqlast);

void lspdecplc(
Word16  *lspq,      /* Q15 */
Word16  *lsppm);    /* Q15 */

Word16 coarsepitch(
Word16 *xw,                /* (i) Q1 weighted low-band signal frame */
struct BV16_Encoder_State *c);   /* (i/o) coder state */

Word16 refinepitch(
Word16   *x,
Word16  cpp,
Word16   *ppt);

Word16 pitchtapquan(
Word16   *x,
Word16   pp,
Word16   *b,
Word32   *re);

void excquan(
Word16   *idx,    /* quantizer codebook index for uq[] vector */
Word16   *s,      /* (i) Q0 input signal vector */
Word16   *aq,     /* (i) Q12 noise feedback filter coefficient array */
Word16   *fsz,    /* (i) Q12 short-term noise feedback filter - numerator */
Word16   *fsp,    /* (i) Q12 short-term noise feedback filter - denominator */
Word16   *b,      /* (i) Q15 coefficient of 3-tap pitch predictor */
Word16   beta,    /* (i) Q13 coefficient of pitch feedback filter */
Word16   *stsym,  /* (i/o) Q0 filter memory */
Word16   *ltsym,  /* (i/0) Q0 long-term synthesis filter memory */
Word16   *ltnfm,  /* (i/o) Q0 long-term noise feedback filter memory */
Word16   *stnfz,  /* (i/o) Q0 filter memory */
Word16   *stnfp,  /* (i/o) Q0 filter memory */
Word16   *cb,     /* (i) scalar quantizer codebook - normalized by gain_exp */
Word16   pp,      /* pitch period (# of 8 kHz samples) */
Word16   gain_exp
);

Word16 gainquan(
Word32  *gainq,      /* Q18 */   
Word32  *ee,         /* Q3 */
Word16  *lgpm,       /* Q11 */
Word32   *prevlg,    /* Q25 */ 
Word32   level);     /* Q25 */

void lspquan(
Word16  *lspq,  
Word16  *lspidx,  
Word16  *lsp,     
Word16  *lsppm);

void preprocess(
struct BV16_Encoder_State *cs,
Word16 *output,                 /* (o) Q0 output signal, less factor 1.5  */
Word16 *input,                  /* (i) Q0 input signal                    */
Word16 N);                      /* length of signal                       */
