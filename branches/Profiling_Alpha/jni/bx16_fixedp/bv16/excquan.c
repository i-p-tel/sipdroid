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
  excquan.c : Vector Quantizer for 2-Stage Noise Feedback Coding 
            with long-term predictive noise feedback coding embedded 
            inside the short-term predictive noise feedback coding loop.

  Note that the Noise Feedback Coding of the excitation signal is implemented
  using the Zero-State Responsse and Zero-input Response decomposition as 
  described in: J.-H. Chen, "Novel Codec Structures for Noise Feedback 
  Coding of Speech," Proc. ICASSP, 2006.  The principle of the Noise Feedback
  Coding of the excitation signal is described in: "BV16 Speech Codec 
  Specification for Voice over IP Applications in Cable Telephony," American 
  National Standard, ANSI/SCTE 24-21 2006.

  Note that indicated Q-values may be relative to the over-all normalization
  by gain_exp.

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "basop32.h"
#include "utility.h"

void vq2snfc_zsr_codebook(
                     Word16   *qzsr,   // normalized by gain_exp-1
                     Word16   *cb,     // normalized by gain_exp
                     Word16   *aq,     // Q12
                     Word16   *fsz,    // Q12
                     Word16   *fsp)    // Q12
{
   Word32 a0, a1;
   Word16 buf1[VDIM], buf2[VDIM], buf3[VDIM];
   Word16 *fp1, *fp2, *fp3, *fpa, *fpb;
   Word16 j, i, n;
   
   /* Q-values of signals are relative to the normalization by gain_exp */
   
   /* Calculate negated Zero State Response */
   fp2 = cb;   /* fp2 points to start of first codevector */
   fp3 = qzsr; /* fp3 points to start of first zero-state response vector */
   
   /* For each codevector */
   for(j=0; j<CBSZ; j++) {
      
      /* Calculate the elements of the negated ZSR */
      for(i=0; i<VDIM; i++){
         /* Short-term prediction */
         a0 = 0;
         fp1 = buf1;                         // Q0
         for (n = i; n > 0; n--)
            a0 = L_msu(a0, *fp1++, aq[n]);   // Q13
         a0 = L_shl(a0, 3);                  // Q16
         
         /* Update memory of short-term prediction filter */
         a0 = L_add(a0, L_deposit_h(*fp2++));
         *fp1++ = intround(a0);
         
         /* noise feedback filter */
         a1 = 0;
         fpa = buf2;                         // Q0
         fpb = buf3;                         // Q0
         for (n = i; n > 0; n--){
            a1 = L_mac(a1, *fpa++, fsz[n]);  // Q13
            a1 = L_msu(a1, *fpb++, fsp[n]);  // Q13
         }
         a1 = L_shl(a1, 3);                  // Q16
         
         /* Update memory of pole section of noise feedback filter */
         *fpb++ = intround(a1);                 // Q0
         
         /* ZSR */
         a0 = L_add(a0, a1);                 // Q16
         
         /* Update memory of zero section of noise feedback filter */
         *fpa++ = negate(intround(a0));
         
         /* Get ZSR at correct normalization - gain_exp-1 */
         *fp3++ = intround(L_shr(a0,1));
      }
   }
   
   return;
}

/* COMPUTE PITCH-PREDICTED VECTOR, WHICH SHOULD BE INDEPENDENT OF THE
RESIDUAL VQ CODEVECTORS BEING TRIED IF vdim < MIN. PITCH PERIOD */

void vq2snfc_ppv(
            Word32   *ltfv,      // Q16
            Word32   *ppv,       // Q16
            Word16   *ltsym,     // Q0
            Word16   *ltnfm,     // Q0
            Word16   *b,         // Q15
            Word16   beta)       // Q13
{
   Word32   a0, a1;
   Word16   n, *sp1;
   for (n = 0; n < VDIM; n++) {
      sp1 = &ltsym[n];                 // Q0
      a0 = L_mult(*sp1--, b[0]);       // Q16
      a0 = L_mac(a0, *sp1--, b[1]);    // Q16
      a0 = L_mac(a0, *sp1--, b[2]);    // Q16
      *ppv++ = a0;                     // Q16
      a1 = L_mult(ltnfm[n], beta);     // Q14
      a1 = L_shl(a1, 2);               // Q16
      *ltfv++ = L_add(a0, a1);         // Q16
   }
   
   return;
}

void vq2snfc_zir(
            Word16   *qzir,
            Word32   *ppv,
            Word32   *ltfv,
            Word16   *aq,
            Word16   *buf1,
            Word16   *buf2,
            Word16   *buf3,
            Word16   *fsz,
            Word16   *fsp,
            Word16   *s,
            Word16   gexpm3)
{
   Word32   a0, a1, a2;
   Word16   i, n;
   Word16   *sp1, *spa, *spb;
   
   for (n = 0; n < VDIM; n++) {
      
      /* Perform multiply-adds along the delay line of filter */
      sp1 = &buf1[n];   /* Q16 */
      a0 = 0;                                       /* Q13 */
      for (i = LPCO; i > 0; i--) a0 = L_msu(a0, *sp1++, aq[i]);
      a0 = L_shl(a0, 3);                                /* Q16 */
      /* Perform multiply-adds along the noise feedback filter */
      spa = &buf2[n];
      spb = &buf3[n];
      a1 = 0;
      for (i=NSTORDER; i > 0; i--) {
         a1 = L_mac(a1, *spa++, fsz[i]);
         a1 = L_msu(a1, *spb++, fsp[i]);
      }
      a1 = L_shl(a1, 3);        /* Q16 */
      *spb = intround(a1);     /* update output of the noise feedback filter */
      a2 = L_deposit_h(s[n]);
      a2 = L_sub(a2, a0);
      a2 = L_sub(a2, a1);       /* Q16 */
      *qzir++ = intround(L_shl(L_sub(a2,*ltfv++),gexpm3));
      /* Update short-term noise feedback filter memory */
      a0 = L_add(a0, *ppv); /* a0 now conatins the qs[n] */
      *sp1 = intround(a0);
      a2 = L_sub(a2, *ppv++);   /* a2 now contains qszi[n] */
      *spa = intround(a2); /* update short-term noise feedback filter memory */
      
   }
   
   return;
}


/* loop through every codevector of the residual vq codebook */
/* and find the one that minimizes the energy of q[n] */

Word16   vq2snfc_vq(
                    Word16   *qzsr,      // normalized by gain_exp - 1
                    Word16   *qzir,      // normalized by gain_exp - 3
                    Word16   *rsign)
{
   Word32   Emin, E;
   Word16   j, n, jmin, sign=1, e; 
   Word16   *fp4, *fp2;
   
   Emin = MAX_32;
   jmin = 0;
   fp4 = qzsr;
   for (j = 0; j < CBSZ; j++) {
      /* Try positive sign */
      fp2 = qzir;
      E = 0;
      for (n=0;n<VDIM;n++){
         e = sub(shl(*fp2++,2), *fp4++);
         E = L_mac0(E, e, e);
      }
      if(L_sub(E, Emin) < 0){
         jmin = j;
         Emin = E;
         sign = 1;
      }
      /* Try negative sign */
      fp4 -= VDIM;
      fp2 = qzir;
      E = 0;
      for (n=0;n<VDIM;n++){
         e = add(shl(*fp2++,2), *fp4++);
         E = L_mac0(E, e, e);
      }
      if(L_sub(E, Emin) < 0){
         jmin = j;
         Emin = E;
         sign = -1;
      }
   }
   
   *rsign = sign;

   return jmin;
}

void vq2snfc_update_mem(
                   Word16 *s,           // Q0
                   Word16 *stsym,       // Q0
                   Word16 *stnfz,       // Q0
                   Word16 *stnfp,       // Q0
                   Word16 *ltsym,       // Q0
                   Word16 *ltnfm,       // Q0
                   Word16 *aq,          // Q12
                   Word16 *fsz,         // Q12
                   Word16 *fsp,         // Q12
                   Word16 *uq,          // normalized by gain_exp
                   Word32 *ppv,         // Q16
                   Word32 *ltfv,        // Q16
                   Word16 gain_exp)
{
   Word16 *fp3, *fp4;
   Word16 *fp1, *fpa, *fpb;
   Word16 *p_uq, *p_s;
   Word16 n, i;
   Word32 *p_ppv, *p_ltfv;
   Word32 a0, a1, v, vq, qs, uq32;
   
   fp3    = ltsym;
   fp4    = ltnfm;
   p_ppv  = ppv;
   p_ltfv = ltfv;
   p_uq   = uq;
   p_s    = s;
   for (n = 0; n < VDIM; n++) {
      
      uq32 = L_shr(L_deposit_h(*p_uq++), gain_exp);
      // Q16
      
      /* Short-term excitation */
      vq = L_add(*p_ppv++, uq32);            // Q16
      /* Update memory of long-term synthesis filter */
      *fp3++ = intround(vq);                    // Q0
      
      /* Short-term prediction */
      a0 = 0;
      fp1 = stsym+n;                         // Q0
      for (i = LPCO; i > 0; i--) 
         a0 = L_msu(a0, *fp1++, aq[i]);      // Q13
      a0 = L_shl(a0, 3);                     // Q16
      
      /* Update memory of short-term synthesis filter */
      *fp1++ = intround(L_add(a0, vq));         // Q0
      
      /* Short-term pole-zero noise feedback filter */
      fpa = stnfz+n;                         // Q0
      fpb = stnfp+n;                         // Q0
      a1 = 0;
      for (i=NSTORDER; i > 0; i--) {
         a1 = L_mac(a1, *fpa++, fsz[i]);     // Q13
         a1 = L_msu(a1, *fpb++, fsp[i]);     // Q13
      }
      a1 = L_shl(a1, 3);                     // Q16
      
      /* Update memory of pole section of noise feedback filter */
      *fpb++ = intround(a1);                    // Q0
      
      v = L_sub(L_sub(L_deposit_h(*p_s++), a0), a1);
      // Q16
      qs = L_sub(v, vq);                     // Q16
      
      /* Update memory of zero section of noise feedback filter */
      *fpa++ = intround(qs);                    // Q0
      
      /* Update memory of long-term noise feedback filter */
      *fp4++ = intround(L_sub(L_sub(v, *p_ltfv++), uq32));  
      // Q0
   }
   
   return;
}

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
             )
{
   Word16 qzir[VDIM];             // normalized by gain_exp-3
   Word16 uq[VDIM];               // normalized by gain_exp
   Word16 buf1[LPCO+FRSZ];        // Q0
   Word16 buf2[NSTORDER+FRSZ];    // Q0
   Word16 buf3[NSTORDER+FRSZ];    // Q0
   Word32 ltfv[VDIM], ppv[VDIM];  // Q16
   Word16 qzsr[VDIM*CBSZ];        // normalized by gain_exp-1
   Word16 *sp3;
   Word16 sign=1;
   Word16 m, n, jmin, iv;
   Word16 gexpm1, gexpm3;
   
   gexpm1 = sub(gain_exp, 1);
   gexpm3 = sub(gain_exp, 3);
   
   /* COPY FILTER MEMORY TO BEGINNING PART OF TEMPORARY BUFFER */
   W16copy(buf1, stsym, LPCO);  /* buffer is used to avoid memory shifts */
   
   /* COPY NOISE FEEDBACK FILTER MEMORY */
   W16copy(buf2, stnfz, NSTORDER);
   W16copy(buf3, stnfp, NSTORDER);
   
   /* -------------------------------------- */ 
   /*  Z e r o - S t a t e  R e s p o n s e  */
   /* -------------------------------------- */
   
   vq2snfc_zsr_codebook(qzsr,cb,aq,fsz,fsp);
   
   /* --------------------------------------------------- */
   /*  LOOP THROUGH EVERY VECTOR OF THE CURRENT SUBFRAME  */
   /* --------------------------------------------------- */
   
   iv = 0;     /* iv = index of the current vector */
   for (m = 0; m < FRSZ; m += VDIM) {
      
      /* --------------------------------------- */
      /*  Z e r o - I n p u t   R e s p o n s e  */
      /* --------------------------------------- */
      
      /* Compute pitch-predicted vector, which should be independent of the
      residual vq codevectors being tried if vdim < min. pitch period */
      vq2snfc_ppv(ltfv,ppv,&ltsym[MAXPP1+m-pp+1],&ltnfm[MAXPP1+m-pp],b,beta); 
      
      /* Compute zero-input response */
      vq2snfc_zir(qzir,ppv,ltfv,aq,&buf1[m],&buf2[m],&buf3[m],fsz,fsp,&s[m],gexpm3);
      
      /* --------------------------------------- */
      /*  C O D E B O O K   S E A R C H          */
      /* --------------------------------------- */
      
      jmin = vq2snfc_vq(qzsr,qzir,&sign);
      
      /* The best codevector has been found; assign vq codebook index */
      idx[iv++] = (sign==-1) ? (jmin+CBSZ) : jmin ;
      
      sp3 = &cb[jmin*VDIM]; /* sp3 points to start of best codevector */
      
      for (n=0;n<VDIM;n++) {
         uq[n] = sign * (*sp3++);   /* Q0 */
      }
      
      /* ----------------------------------------- */
      /*  U p d a t e   F i l t e r   M e m o r y  */
      /* ----------------------------------------- */
      vq2snfc_update_mem(s+m,buf1+m,buf2+m,buf3+m,ltsym+MAXPP1+m,ltnfm+MAXPP1+m,aq,fsz,fsp,uq,ppv,ltfv,gain_exp);
      
   }
   
   /* Update noise feedback filter memory after filtering current subframe */
   W16copy(stsym, buf1+FRSZ, LPCO);
   W16copy(stnfz, buf2+FRSZ, NSTORDER);
   W16copy(stnfp, buf3+FRSZ, NSTORDER);
   
   return;
}

