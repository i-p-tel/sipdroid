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
  bv16.h : 

  $Log$
******************************************************************************/

void Reset_BV16_Encoder(
struct BV16_Encoder_State *cs);

void BV16_Encode(
struct BV16_Bit_Stream *bs,
struct BV16_Encoder_State *cs,
Word16 *inx);

void Reset_BV16_Decoder(
struct BV16_Decoder_State *ds);

void BV16_Decode(
struct BV16_Bit_Stream     *bs,
struct BV16_Decoder_State  *ds,
Word16 *xq);

void BV16_PLC(
struct BV16_Decoder_State   *ds,
Word16 *x);

