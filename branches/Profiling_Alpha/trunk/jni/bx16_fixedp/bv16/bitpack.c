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
  bitpack.c: BV16 bit packing routines

  $Log$
******************************************************************************/

#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"
#include "bv16.h"

/****************************************************************************
*
*  BV16_BITPACK - BroadVoice16 Encoded Bit Pack Function
*
*  PURPOSE:
*
*     This function take the encoded bit structure (15 words) and packed it 
*     into a bit stream (5 words) for sending it across the network.
*
*  PROTOTYPE:
*     BV16_BitPack ( UINT16 * PackedStream, sBV16bs * BitStruct );
*
*  PARAMETERS:
*     PackedStream  => pointer to the outgoing encoded stream
*     BitStruct     => pointer to the encoded bit structure
*
*  RETURNS:
*     Nothing
*
*  NOTE:
*     The following is the bit table within the bit structure for 
*     BroadVoice16
*
*     Word16  bit_table[] = {
*        7, 7,                           // LSP 
*        7,                              // Pitch Lag 
*        5,                              // Pitch Gain 
*        4,                              // Excitation Vector Log-Gain 
*        5, 5, 5, 5, 5, 5, 5, 5, 5, 5    // Excitation Vector   
*      };
*
****************************************************************************/
void BV16_BitPack (UWord8 * PackedStream, struct BV16_Bit_Stream * BitStruct )
{
   UWord32 temppack;
   
   /* fill the first 16 bit word */
   temppack = ( ((UWord32)BitStruct->lspidx[0]) << 25 );  /* 32-7 */
   temppack |= ( ((UWord32)BitStruct->lspidx[1]) << 18 ); /* 25-7 */
   temppack |= ( ((UWord32)BitStruct->ppidx) << 11 );     /* 18-7 */
   /* total=21 */ 

   /* store 1st byte in the payload */
   *PackedStream++ = (UWord8)(temppack >> 24);

   /* store 2nd byte in the payload */
   *PackedStream++ = (UWord8)((temppack << 8) >> 24);
   
   /* clear the upper 16 bits */
   temppack = temppack << 16;
   
   temppack |= ( ((UWord32)BitStruct->bqidx) << 22 );    /* 32-(21-16)-5 = 32-10 */
   temppack |= ( ((UWord32)BitStruct->gidx) << 18 );     /* 22-4 */
   temppack |= ( ((UWord32)BitStruct->qvidx[0]) << 13 ); /* 18-5 */
   /* total=19 */ 

   /* store 3rd byte in the payload */
   *PackedStream++ = (UWord8)(temppack >> 24);

   /* store 4th byte in the payload */
   *PackedStream++ = (UWord8)((temppack << 8) >> 24);
   
   /* clear the upper 16 bits */
   temppack = temppack << 16;
   
   temppack |= ( ((UWord32)BitStruct->qvidx[1]) << 24 ); /* 32-(19-16)-5 = 32-8 */
   temppack |= ( ((UWord32)BitStruct->qvidx[2]) << 19 ); /* 24-5 */
   temppack |= ( ((UWord32)BitStruct->qvidx[3]) << 14 ); /* 19-5 */
   /* total=18 */    
   
   /* store 5th byte in the payload */
   *PackedStream++ = (UWord8)(temppack >> 24);

   /* store 6th byte in the payload */
   *PackedStream++ = (UWord8)((temppack << 8) >> 24);
   
   /* clear the upper 16 bits */
   temppack = temppack << 16;
   
   temppack |= ( ((UWord32)BitStruct->qvidx[4]) << 25 ); /* 32-(18-16)-5 = 32-7 */
   temppack |= ( ((UWord32)BitStruct->qvidx[5]) << 20 ); /* 25-5 */
   temppack |= ( ((UWord32)BitStruct->qvidx[6]) << 15 ); /* 20-5 */
   /* total=17 */    
   
   /* store 7th byte in the payload */
   *PackedStream++ = (UWord8)(temppack >> 24);

   /* store 8th byte in the payload */
   *PackedStream++ = (UWord8)((temppack << 8) >> 24);
   
   /* clear the upper 16 bits */
   temppack = temppack << 16;
   
   temppack |= ( ((UWord32)BitStruct->qvidx[7]) << 26 ); /* 32-(17-16)-5 = 32-6 */
   temppack |= ( ((UWord32)BitStruct->qvidx[8]) << 21 ); /* 25-5 */
   temppack |= ( ((UWord32)BitStruct->qvidx[9]) << 16 ); /* 20-5 */
   /* total=18 */ 

   /* store 9th byte in the payload */
   *PackedStream++ = (UWord8)(temppack >> 24);

   /* store 10th byte in the payload */
   *PackedStream++ = (UWord8)((temppack << 8) >> 24);

   return;
}


/****************************************************************************
*
*  BV16_BITUNPACK - BroadVoice16 Encoded Bit Unpack Function
*
*  PURPOSE:
*
*     This function take the encoded bit stream (5 words) and unpack it 
*     into a bit structure (15 words) for BroadVoice16 decoder.
*
*  PROTOTYPE:
*     BV16_BitUnPack ( UINT16 * PackedStream, sBV16bs * BitStruct );
*
*  PARAMETERS:
*     PackedStream  => pointer to the incoming encoded bit stream
*     BitStruct     => pointer to the  bit structure for decoder
*
*  RETURNS:
*     Nothing
*
*  NOTE:
*     The following is the bit table within the bit structure for 
*     BroadVoice16
*
*     Word16  bit_table[] = {
*        7, 7,                           // LSP 
*        7,                              // Pitch Lag 
*        5,                              // Pitch Gain 
*        4,                              // Excitation Vector Log-Gain 
*        5, 5, 5, 5, 5, 5, 5, 5, 5, 5    // Excitation Vector   
*      };
*
*
****************************************************************************/
void BV16_BitUnPack (UWord8 * PackedStream, struct BV16_Bit_Stream * BitStruct )
{
   UWord32 bitword32;
   
   /* unpack bytes 1 and 2 of bit stream */
   bitword32 = (UWord32)*PackedStream++;
   bitword32 = (bitword32 << 8) | (UWord32)*PackedStream++;
   BitStruct->lspidx[0] = (short)( bitword32 >> 9 ); 
   BitStruct->lspidx[1] = (short)( ( bitword32 & 0x01FF )>> 2 );
   
   /* unpack bytes 3 and 4 of bit stream */
   bitword32 = ((bitword32 & 0x0003) << 8) | (UWord32) *PackedStream++;
   bitword32 = (bitword32 << 8) | (UWord32)*PackedStream++;
   BitStruct->ppidx = (short)( bitword32 >> 11 );
   BitStruct->bqidx = (short)( ( bitword32 & 0x07FF ) >> 6 );
   BitStruct->gidx = (short)( ( bitword32 & 0x003F ) >> 2 );
   
   /* unpack bytes 5 and 6 of bit stream */
   bitword32 = ((bitword32 & 0x0003) << 8) | (UWord32) *PackedStream++;
   bitword32 = (bitword32 << 8) | (UWord32)*PackedStream++;
   BitStruct->qvidx[0] = (short)( bitword32 >> 13 );
   BitStruct->qvidx[1] = (short)( ( bitword32 & 0x1FFF ) >> 8 );
   BitStruct->qvidx[2] = (short)( ( bitword32 & 0x00FF ) >> 3 );
   
   /* unpack bytes 7 and 8 of bit stream */
   bitword32 = ((bitword32 & 0x0007) << 8) | (UWord32) *PackedStream++;
   bitword32 = (bitword32 << 8) | (UWord32)*PackedStream++;
   BitStruct->qvidx[3] = (short)( bitword32 >> 14 );
   BitStruct->qvidx[4] = (short)( ( bitword32 & 0x3FFF ) >> 9 );
   BitStruct->qvidx[5] = (short)( ( bitword32 & 0x01FF ) >> 4 );
   
   /* unpack bytes 9 and 10 of bit stream */
   bitword32 = ((bitword32 & 0x000F) << 8) | (UWord32) *PackedStream++;
   bitword32 = (bitword32 << 8) | (UWord32)*PackedStream++;
   BitStruct->qvidx[6] = (short)( ( bitword32 >> 15 ) );
   BitStruct->qvidx[7] = (short)( ( bitword32 & 0x7FFF ) >> 10 );
   BitStruct->qvidx[8] = (short)( ( bitword32 & 0x03FF ) >> 5 );
   BitStruct->qvidx[9] = (short)( bitword32 & 0x001F );
   
   return;
}
