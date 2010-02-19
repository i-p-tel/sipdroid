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
  g192.c : Implementation of optional G.192 bit-stream format

  $Log$
******************************************************************************/

#include <stdio.h>
#include "typedef.h"
#include "bvcommon.h"
#include "bv16cnst.h"
#include "bv16strct.h"

#define NBIT        80                  /* number of bits per frame */
#define BIT_0     (short)0x007f
#define BIT_1     (short)0x0081
#define SYNC_WORD (short)0x6b21

extern Word16  bfi;

Word16   bit_table_16[] = { 
   7, 7,                   /* LSP */
      7,                      /* Pitch Lag */
      5,                      /* Pitch Gain */
      4,                      /* Excitation Vector Log-Gain */
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5  /* Excitation Vector */
};

#define  Nindices 15          /* number of Q indices per frame */

Word16 bin2int_16(Word16 no_of_bits, Word16 *bitstream)
{
   Word16 index, b_pos;

   index = 0;
   for(b_pos=0; b_pos<no_of_bits; b_pos++){
      index = index << 1;
      if(bitstream[b_pos] == BIT_1)
         index++;
   }

   return index;
}

void int2bin_16(Word16 index, Word16 no_of_bits, Word16 *bitstream)
{
   Word16 b_pos;

   for(b_pos=0; b_pos<no_of_bits; b_pos++){
      if(index & 1)
         bitstream[no_of_bits-1-b_pos] = BIT_1;
      else
         bitstream[no_of_bits-1-b_pos] = BIT_0;
      index = index >> 1;
   }

   return;
}

void bv16_fwrite_g192bitstrm(struct BV16_Bit_Stream *bs, FILE *fo)
{
   Word16 n, m;
   Word16 bitstream[NBIT+2], *p_bitstream, *pbs;
   
   bitstream[0] = SYNC_WORD;
   bitstream[1] = NBIT;
   p_bitstream = bitstream + 2;
   
   pbs = (Word16 *) bs;
   
   for (n=0;n<Nindices;n++) 
   {
      m = bit_table_16[n];
      int2bin_16(*pbs++, m, p_bitstream);
      p_bitstream += m;
   }
   
   fwrite(bitstream, sizeof(Word16), NBIT+2, fo);
   
   return;
}

/* function to read bit-stream in G.192 compliant format */
Word16 bv16_fread_g192bitstrm(struct BV16_Bit_Stream *bs, FILE *fi)
{
   Word16 sync_word, n, m, nread;
   Word16 bitstream[NBIT+1], *p_bitstream;
   Word16 *pbs;
   
   nread=fread(&sync_word, sizeof(Word16), 1, fi);
   if(sync_word == SYNC_WORD)
      bfi = 0;
   else
      bfi = 1;
   
   fread(bitstream, sizeof(Word16), NBIT+1, fi);
   p_bitstream = bitstream + 1;
   
   pbs = (Word16 *) bs;
   
   /* LSP indices */
   for (n=0;n<Nindices;n++) 
   {
      m = bit_table_16[n]; 
      *pbs++ = bin2int_16(m, p_bitstream);
      p_bitstream += m;
   }
   
   return nread;
}

