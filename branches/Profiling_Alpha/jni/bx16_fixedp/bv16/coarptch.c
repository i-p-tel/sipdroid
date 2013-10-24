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
  coarptch.c: Coarse pitch search

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16externs.h"
#include "basop32.h"


Word16 coarsepitch(
                   Word16  *xw,           /* (i) (normalized) weighted signal */
                   struct BV16_Encoder_State *cstate) /* (i/o) Coder State */
{
   
   Word16   s;    /* Q2 */
   Word16   a, b; 
   Word16   im;
   Word16   maxdev, flag, mpflag;
   Word32   eni, deltae;
   Word32   cc;
   Word16   ah,al, bh, bl;
   Word32   a0, a1, a2, a3;
   Word32   *lp0; 
   Word16   exp, new_exp;  
   Word16   *fp0, *fp1, *fp2, *fp3, *sp; 
   Word16   *fp1_h, *fp1_l, *fp2_h, *fp2_l;
   Word16   cor2max, cor2max_exp;
   Word16   cor2m, cor2m_exp;
   Word16   s0, t0, t1, exp0, exp1, e2, e3;
   Word16   threshold;
   Word16   mplth;      /* Q2 */
   Word16   i, j, k, n, npeaks, imax, idx[MAXPPD-MINPPD+1];
   Word16   cpp;
   Word16 plag[HMAXPPD], cor2[MAXPPD1], cor2_exp[MAXPPD1];
   Word16 cor2i[HMAXPPD], cor2i_exp[HMAXPPD], xwd[LXD];
   Word16 tmp_h[DFO+FRSZ], tmp_l[DFO+FRSZ]; /* DPF Q7 */
   Word32 cor[MAXPPD1], energy[MAXPPD1], lxwd[FRSZD];
   Word16 energy_man[MAXPPD1], energy_exp[MAXPPD1];
   Word16 energyi_man[HMAXPPD], energyi_exp[HMAXPPD];
   Word16 energym_man, energym_exp;
   Word16 energymax_man, energymax_exp;
      
   /* Lowpass filter xw() to 800 hz; shift & output into xwd() */
   
   /* AP and AZ filtering and decimation */
   
   fp1_h = tmp_h + DFO;
   fp1_l = tmp_l + DFO;
   
   sp = xw;
   a1 = 1;
   
   
   for (i=0;i<DFO;i++) tmp_h[i] = cstate->dfm_h[2*i+1];
   for (i=0;i<DFO;i++) tmp_l[i] = cstate->dfm_h[2*i];
   
   lp0 = lxwd;
   
   for (i=0;i<FRSZD;i++) {
      for (k=0;k<DECF;k++) {
         a0 = L_shr(L_deposit_h(*sp++),10);
         fp2_h = fp1_h-1;
         fp2_l = fp1_l-1;
         for (j=0;j<DFO;j++)  
            a0=L_sub(a0,Mpy_32(*fp2_h--,*fp2_l--,adf_h[j+1],adf_l[j+1]));
         a0 = L_shl(a0, 2);             /* adf Q13 */
         L_Extract(a0, fp1_h++, fp1_l++);
      }
      fp2_h = fp1_h-1;
      fp2_l = fp1_l-1;
      a0 = Mpy_32_16(*fp2_h--, *fp2_l--, bdf[0]);
      for (j=0;j<DFO;j++)
         a0=L_add(a0,Mpy_32_16(*fp2_h--,*fp2_l--,bdf[j+1]));
      
      *lp0++ = a0;
      a0 = L_abs(a0);
      
      if (a1 < a0) 
         a1 = a0;
   }
   
   /* copy temp buffer to memory */
   fp1_h -= DFO;
   fp1_l -= DFO;
   for (i=0;i<DFO;i++) {
      cstate->dfm_h[2*i+1] = fp1_h[i];
      cstate->dfm_h[2*i] = fp1_l[i];
   }
   
   lp0 = lxwd;
   new_exp = sub(norm_l(a1), 3);       /* headroom to avoid overflow */
   exp = sub(cstate->xwd_exp,new_exp); /* increase in bit-resolution */
   
   if (exp < 0) { /* Descending signal level */
      new_exp = cstate->xwd_exp;
      exp = 0;
   }
   
   for (i=0;i<XDOFF;i++) 
      xwd[i] = shr(cstate->xwd[i], exp);
   
   /* fill-in new exponent */
   fp0 = xwd + XDOFF;
   for (i=0;i<FRSZD;i++) 
      fp0[i] = intround(L_shl(lp0[i],new_exp));
   
   /* update signal memory for next frame */
   exp0 = 1;
   for (i=0;i<XDOFF;i++) {
      exp1 = abs_s(xwd[FRSZD+i]);
      
      if (exp1 > exp0) 
         exp0 = exp1;
   }
   exp0 = sub(norm_s(exp0),3); /* extra exponent for next frame */
   
   exp = sub(exp0, exp);
   
   if (exp >=0)
   {
      for (i=0;i<XDOFF-FRSZD;i++)  
         cstate->xwd[i] = shl(cstate->xwd[i+FRSZD], exp);
   }
   else
   {
      exp = -exp;
      if (exp >=15)
         exp = 15;
      for (i=0;i<XDOFF-FRSZD;i++)  
         cstate->xwd[i] = shr(cstate->xwd[i+FRSZD], exp);
   }
   for (;i<XDOFF;i++) 
      cstate->xwd[i] = shl(xwd[FRSZD+i],exp0);
   
   
   cstate->xwd_exp = add(new_exp, exp0);
   
   /* Compute correlation & energy of prediction basis vector */
   
   /* reset local buffers */
   for (i=0;i<MAXPPD1;i++) 
      cor[i] = energy[i] = 0;
   
   fp0 = xwd+MAXPPD1;
   fp1 = xwd+MAXPPD1-M1;
   a0 = a1 = 0; 
   for (i=0;i<(LXD-MAXPPD1);i++) {
      a0 = L_mac0(a0, *fp1, *fp1);
      a1 = L_mac0(a1, *fp0++, *fp1++);
   }
   cor[M1-1] = a1;
   energy[M1-1] = a0;
   energy_exp[M1-1] = norm_l(energy[M1-1]);
   energy_man[M1-1] = extract_h(L_shl(energy[M1-1], energy_exp[M1-1]));
   s0 = cor2_exp[M1-1] = norm_l(a1);
   t0 = extract_h(L_shl(a1, s0));
   cor2[M1-1] = extract_h(L_mult(t0, t0));
   
   if (a1 < 0) 
      cor2[M1-1] = negate(cor2[M1-1]);
   
   fp2 = xwd+LXD-M1-1;
   fp3 = xwd+MAXPPD1-M1-1;
   for (i=M1;i<M2;i++) {
      fp0 = xwd+MAXPPD1;
      fp1 = xwd+MAXPPD1-i-1;
      a1 = 0;
      for (j=0;j<(LXD-MAXPPD1);j++) 
         a1 = L_mac0(a1,*fp0++,*fp1++); 
      cor[i] = a1;
      a0 = L_msu0(a0, *fp2, *fp2);
      a0 = L_mac0(a0, *fp3, *fp3);
      fp2--; fp3--;
      energy[i] = a0;
      energy_exp[i] = norm_l(energy[i]);
      energy_man[i] = extract_h(L_shl(energy[i], energy_exp[i]));
      s0 = cor2_exp[i] = norm_l(a1);
      t0 = extract_h(L_shl(a1, s0));
      cor2[i] = extract_h(L_mult(t0, t0));
      
      if (a1 < 0) 
         cor2[i] = negate(cor2[i]);
   }
   
   /* Find positive correlation peaks */
   /* Find maximum of cor*cor/energy among positive correlation peaks */ 
   
   npeaks = 0;
   n = MINPPD-1;
   while ((npeaks < MAX_NPEAKS) && (n<MAXPPD)) {
      
      if (cor[n]>0) { 
         a0   = L_mult(energy_man[n-1],cor2[n]);
         a1   = L_mult(energy_man[n], cor2[n-1]);
         exp0 = shl(sub(cor2_exp[n], cor2_exp[n-1]),1);
         exp0 = add(exp0, energy_exp[n-1]);
         exp0 = sub(exp0, energy_exp[n]);
         
         if (exp0>=0) 
            a0 = L_shr(a0, exp0);
         else 
            a1 = L_shl(a1, exp0);
         
         if (a0 > a1) { 
            
            a0   = L_mult(energy_man[n+1],cor2[n]);
            a1   = L_mult(energy_man[n], cor2[n+1]);
            exp0 = shl(sub(cor2_exp[n], cor2_exp[n+1]),1);
            exp0 = add(exp0, energy_exp[n+1]);
            exp0 = sub(exp0, energy_exp[n]);
            
            if (exp0>=0) 
               a0 = L_shr(a0, exp0);
            else 
               a1 = L_shl(a1, exp0);
            
            if (a0 > a1) {
               idx[npeaks] = n;
               npeaks++; 
            }
         }
    }
    
    n++;
    
  }
  
  /* Return early if there is no peak or only one peak */
  
  if (npeaks == 0){   /* if there are no positive peak, */
     return MINPPD*DECF; /* return minimum pitch period in decimated domain */
  }
  
  if (npeaks == 1){   /* if there is exactly one peak, */
     return (idx[0]+1)*DECF; /* return the time lag for this single peak */
  }
  
  /* If program proceeds to here, there are 2 or more peaks */
  cor2max=(Word16) 0x8000;
  cor2max_exp= (Word16) 0;
  energymax_man=1;
  energymax_exp=0;
  
  imax=0;
  for (i=0; i < npeaks; i++) {
     
  /* Use quadratic interpolation to find the interpolated cor[] and
     energy[] corresponding to interpolated peak of cor2[]/energy[] */
     /* first calculate coefficients of y(x)=ax^2+bx+c; */
     n=idx[i];
     a0=L_sub(L_shr(L_add(cor[n+1],cor[n-1]),1),cor[n]);
     L_Extract(a0, &ah, &al);
     a0=L_shr(L_sub(cor[n+1],cor[n-1]),1);
     L_Extract(a0, &bh, &bl);
     cc=cor[n];
     
     /* Initialize variables before searching for interpolated peak */
     im=0;
     cor2m_exp = cor2_exp[n];
     cor2m = cor2[n];
     energym_exp = energy_exp[n];
     energym_man = energy_man[n];
     eni=energy[n];
     
     /* Determine which side the interpolated peak falls in, then
     do the search in the appropriate range */
     
     a0   = L_mult(energy_man[n-1],cor2[n+1]);
     a1   = L_mult(energy_man[n+1], cor2[n-1]);
     exp0 = shl(sub(cor2_exp[n+1], cor2_exp[n-1]),1);
     exp0 = add(exp0, energy_exp[n-1]);
     exp0 = sub(exp0, energy_exp[n+1]);
     
     if (exp0>=0) 
        a0 = L_shr(a0, exp0);
     else 
        a1 = L_shl(a1, exp0);
     
     if (a0 > a1) {  /* if right side */
        
        deltae = L_shr(L_sub(energy[n+1], eni), 2);
        
        for (k = 0; k < HDECF; k++) {
           a0=L_add(L_add(Mpy_32_16(ah,al,x2[k]),Mpy_32_16(bh,bl,x[k])),cc);
           eni = L_add(eni, deltae);
           a1 = eni;
           exp0 = norm_l(a0);
           s0 = extract_h(L_shl(a0, exp0));
           s0 = extract_h(L_mult(s0, s0));
           e2 = energym_exp;
           t0 = energym_man;
           a2 = L_mult(t0, s0);
           e3 = norm_l(a1);
           t1 = extract_h(L_shl(a1, e3));
           a3 = L_mult(t1, cor2m);
           exp1 = shl(sub(exp0, cor2m_exp),1);
           exp1 = add(exp1, e2);
           exp1 = sub(exp1, e3);
           
           if (exp1>=0) 
              a2 = L_shr(a2, exp1);
           else 
              a3 = L_shl(a3, exp1);
           
           if (a2 > a3) {
              im = k+1;
              cor2m = s0;
              cor2m_exp = exp0;
              energym_exp = e3;
              energym_man = t1;
           }
        }        
     } else {    /* if interpolated peak is on the left side */
        
        deltae = L_shr(L_sub(energy[n-1], eni), 2);
        for (k = 0; k < HDECF; k++) {
           a0=L_add(L_sub(Mpy_32_16(ah,al,x2[k]),Mpy_32_16(bh,bl,x[k])),cc);
           eni = L_add(eni, deltae);
           a1=eni;
           
           exp0 = norm_l(a0);
           s0 = extract_h(L_shl(a0, exp0));
           s0 = extract_h(L_mult(s0, s0));
           e2 = energym_exp;
           t0 = energym_man;
           a2 = L_mult(t0, s0);
           e3 = norm_l(a1);
           t1 = extract_h(L_shl(a1, e3));
           a3 = L_mult(t1, cor2m);
           exp1 = shl(sub(exp0, cor2m_exp),1);
           exp1 = add(exp1, e2);
           exp1 = sub(exp1, e3);
           
           if (exp1>=0) 
              a2 = L_shr(a2, exp1);
           else 
              a3 = L_shl(a3, exp1);
           
           if (a2 > a3) {
              im = -k-1;
              cor2m = s0;
              cor2m_exp = exp0;
              energym_exp = e3;
              energym_man = t1;
           }
        }        
     }
     
     /* Search done; assign cor2[] and energy[] corresponding to 
     interpolated peak */ 
     plag[i]=add(shl(add(idx[i],1),2),im); /* lag of interp. peak */
     cor2i[i]=cor2m;
     cor2i_exp[i]=cor2m_exp;
     /* interpolated energy[] of i-th interpolated peak */
     energyi_exp[i] = energym_exp;
     energyi_man[i] = energym_man;
     
     /* Search for global maximum of interpolated cor2[]/energy[] peak */
     a0 = L_mult(cor2m,energymax_man);
     a1 = L_mult(cor2max, energyi_man[i]);
     exp0 = shl(sub(cor2m_exp, cor2max_exp),1);
     exp0 = add(exp0, energymax_exp);
     exp0 = sub(exp0, energyi_exp[i]);
     
     if (exp0 >=0) 
        a0 = L_shr(a0, exp0);
     else 
        a1 = L_shl(a1, exp0);
     
     if (a0 > a1) {
        imax=i;
        cor2max=cor2m;
        cor2max_exp=cor2m_exp;
        energymax_exp = energyi_exp[i];
        energymax_man = energyi_man[i];
     }
  }
  cpp=plag[imax]; /* first candidate for coarse pitch period */
  mplth=plag[npeaks-1]; /* set mplth to the lag of last peak */
  
  /* Find the largest peak (if there is any) around the last pitch */
  maxdev= shr(cstate->cpplast,2); /* maximum deviation from last pitch */
  im = -1;
  cor2m=(Word16) 0x8000;
  cor2m_exp= (Word16) 0;
  energym_man = 1;
  energym_exp = 0;
  
  for (i=0;i<npeaks;i++) {  /* loop thru the peaks before the largest peak */
     
     if (abs_s(sub(plag[i],cstate->cpplast)) <= maxdev) {
        a0 = L_mult(cor2i[i],energym_man);
        a1 = L_mult(cor2m, energyi_man[i]);
        exp0 = shl(sub(cor2i_exp[i], cor2m_exp),1);
        exp0 = add(exp0, energym_exp);
        exp0 = sub(exp0, energyi_exp[i]);
        
        if (exp0 >=0) 
           a0 = L_shr(a0, exp0);
        else 
           a1 = L_shl(a1, exp0);
        
        if (a0 > a1) {
           im=i;
           cor2m=cor2i[i];
           cor2m_exp=cor2i_exp[i];
           energym_man = energyi_man[i];
           energym_exp = energyi_exp[i];
        }   
     }
  } /* if there is no peaks around last pitch, then im is still -1 */
  
  
  /* Now see if we should pick any alternatice peak */
  /* first, search first half of pitch range, see if any qualified peak
  has large enough peaks at every multiple of its lag */
  i=0;
  
  while (2*plag[i] < mplth) {
     
     /* Determine the appropriate threshold for this peak */
     
     if (i != im) {  /* if not around last pitch, */
        threshold = TH1;    /* use a higher threshold */
     } else {        /* if around last pitch */
        threshold = TH2;    /* use a lower threshold */
     }
     
     /* If threshold exceeded, test peaks at multiples of this lag */
     a0 = L_mult(cor2i[i],energymax_man);
     t1 = extract_h(L_mult(energyi_man[i], threshold));
     a1 = L_mult(cor2max, t1);
     exp0 = shl(sub(cor2i_exp[i], cor2max_exp),1);
     exp0 = add(exp0, energymax_exp);
     exp0 = sub(exp0, energyi_exp[i]);
     
     if (exp0 >=0) 
        a0 = L_shr(a0, exp0);
     else 
        a1 = L_shl(a1, exp0);
     
     if (a0 > a1) {
        flag=1;  
        j=i+1;
        k=0;
        s=shl(plag[i],1); /* initialize t to twice the current lag */
        
        while (s<=mplth) { /* loop thru all multiple lag <= mplth */
           
           mpflag=0;   /* initialize multiple pitch flag to 0 */
           t0 = mult_r(s,MPDTH); 
           a=sub(s, t0);   /* multiple pitch range lower bound */
           b=add(s, t0);   /* multiple pitch range upper bound */
           while (j < npeaks) { /* loop thru peaks with larger lags */
              
              if (plag[j] > b) { /* if range exceeded, */
                 break;          /* break the innermost while loop */
              }       /* if didn't break, then plag[j] <= b */
              
              if (plag[j] > a) { /* if current peak lag within range, */
                 /* then check if peak value large enough */
                 a0 = L_mult(cor2i[j],energymax_man);
                 if (k<4) 
                    t1 = MPTH[k];
                 else 
                    t1 = MPTH4;
                 t1 = extract_h(L_mult(t1, energyi_man[j]));
                 a1 = L_mult(cor2max, t1);
                 exp0 = shl(sub(cor2i_exp[j], cor2max_exp),1);
                 exp0 = add(exp0, energymax_exp);
                 exp0 = sub(exp0, energyi_exp[j]);
                 
                 if (exp0 >=0) 
                    a0 = L_shr(a0, exp0);
                 else 
                    a1 = L_shl(a1, exp0);
                 
                 if (a0 > a1) {
                    mpflag=1; /* if peak large enough, set mpflag, */
                    break; /* and break the innermost while loop */
                 } 
              }
              j++;
           }
           /* if no qualified peak found at this multiple lag */
           
           if (mpflag == 0) { 
              flag=0;     /* disqualify the lag plag[i] */
              break;      /* and break the while (s<=mplth) loop */
           }
           k++;
           s = add(s, plag[i]); /* update s to the next multiple pitch lag */
           
        }
        /* if there is a qualified peak at every multiple of plag[i], */
        
        if (flag == 1) { 
           cpp = plag[i];   /* then accept this as final pitch */

           return cpp;         /* and return to calling function */
        }
    }       
    i++;
    
    if (i == npeaks)
       break;      /* to avoid out of array bound error */
  }
  
  /* If program proceeds to here, none of the peaks with lags < 0.5*mplth
  qualifies as the final pitch. in this case, check if
  there is any peak large enough around last pitch.  if so, use its
  lag as the final pitch. */
  
  if (im != -1) {   /* if there is at least one peak around last pitch */
     
     if (im == imax) { /* if this peak is also the global maximum, */
        return cpp;   /* return first pitch candidate at global max */
     }
     
     if (im < imax) { /* if lag of this peak < lag of global max, */
        a0 = L_mult(cor2m,energymax_man);
        t1 = extract_h(L_mult(energym_man, LPTH2));
        a1 = L_mult(cor2max, t1);
        exp0 = shl(sub(cor2m_exp, cor2max_exp),1);
        exp0 = add(exp0, energymax_exp);
        exp0 = sub(exp0, energym_exp);
        
        if (exp0 >=0) 
           a0 = L_shr(a0, exp0);
        else 
           a1 = L_shl(a1, exp0);
        
        if (a0 > a1) {
           
           if (plag[im] > HMAXPPD*DECF) {
              cpp=plag[im];
              
              return cpp;
           }
           for (k=2; k<=5;k++) { /* check if current candidate pitch */
              s=mult(plag[imax],invk[k-2]); /* is a sub-multiple of */
              t0 = mult_r(s,SMDTH);
              a=sub(s, t0);      /* the time lag of */
              b=add(s, t0);       /* the global maximum peak */
              
              if (plag[im]>a && plag[im]<b) {     /* if so, */
                 cpp=plag[im];       /* accept this peak, */
                 
                 return cpp;         /* and return as pitch */
              }
           }
        }
     } else {           /* if lag of this peak > lag of global max, */
        a0 = L_mult(cor2m,energymax_man);
        t1 = extract_h(L_mult(energym_man, LPTH1));
        a1 = L_mult(cor2max, t1);
        exp0 = shl(sub(cor2m_exp, cor2max_exp),1);
        exp0 = add(exp0, energymax_exp);
        exp0 = sub(exp0, energym_exp);
        
        if (exp0 >=0) 
           a0 = L_shr(a0, exp0);
        else 
           a1 = L_shl(a1, exp0);
        
        if (a0 > a1) {
           cpp = plag[im];  /* if this peak is large enough, */
           
           return cpp;         /* accept its lag */ 
        }
     }
  }
  
  /* If program proceeds to here, we have no choice but to accept the
  lag of the global maximum */
  return cpp;
  
}
