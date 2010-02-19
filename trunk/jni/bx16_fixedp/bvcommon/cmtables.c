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
  cmtables.c : Common Fixed-Point Library: tables

  $Log$
******************************************************************************/

#include "typedef.h"

/* LPC bandwidth expansion */

Word16   bwel[] = {
   32767,  31737,  30738,  29770,  28833,  27925,  27046,  26195,  25370
};

/* LPC WEIGHTING filter */
Word16   STWAL[] = {
   32767,  24576,  18432,  13824,  10368,   7776,   5832,   4374,   3281
};

/* coarse pitch search */
Word16   invk[4] = { 16384, 10923,  8192,  6554 };

/* LPC -> LSP Conversion */

Word16   grid[] ={
  32766,  32557,  32272,  31868,  31385,  30832,
  30197,  29478,  28684,  27813,  26864,  25846,
  24769,  23637,  22459,  21238,  19993,  18710,
  17370,  15999,  14574,  13086,  11571,  10021,
   8473,   6913,   5344,   3764,   2154,    529,
  -1100,  -2723,  -4324,  -5912,  -7470,  -9016,
 -10566, -12102, -13618, -15087, -16496, -17847,
 -19166, -20434, -21667, -22872, -24041, -25152,
 -26211, -27204, -28129, -28975, -29744, -30437,
 -31038, -31572, -32021, -32385, -32621, -32766
}; 
