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
  fixmath.h : Common Fixed-Point Library: 

  $Log$
******************************************************************************/

Word32 Pow2(                  /* Q0 output            */
            Word16 int_comp,  /* Q0 Integer part      */
            Word16 frac_comp  /* Q15 frac_compal part  */
            );

void Log2(
          Word32 x,         /* (i) input           */
          Word16 *int_comp, /* Q0 integer part     */
          Word16 *frac_comp /* Q15 fractional part */
          );

void sqrt_i(Word16 x_man, Word16 x_exp, Word16 *y_man, Word16 *y_exp);
Word16 sqrts(Word16 x);

extern Word16 tabsqrt[];
extern Word16 tablog[];
extern Word16 tabpow[];
extern Word16 costable[];
extern Word16 acosslope[];
