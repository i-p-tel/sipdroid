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
  memutil.c : Common Fixed-Point Library: memory utilities

  $Log$
******************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include "typedef.h"

/* allocate a Word16 vector with subscript range v[nl...nh] */
/* from numerical recipes in C 2nd edition, page 943        */
Word16 *allocWord16(long nl, long nh)
{
   Word16 *v;
   
   v = (Word16 *)malloc((size_t)((nh-nl+1)*sizeof(Word16)));
   if (!v){
      printf("Memory allocation error in allocWord16()\n");
      exit(0);
   }
   
   return v-nl;
}

/* free a Word16 vector allocated by svector()              */
/* from numerical recipes in C 2nd edition, page 946        */
void deallocWord16(Word16 *v, long nl, long nh)
{
   
   free((char *)(v+nl));
   
   return;
}

/* allocate a Word32 vector with subscript range v[nl...nh] */
/* from numerical recipes in C 2nd edition, page 943        */
Word32 *allocWord32(long nl, long nh)
{
   Word32 *v;
   
   v = (Word32 *)malloc((size_t)((nh-nl+1)*sizeof(Word32)));
   if (!v){
      printf("Memory allocation error in allocWord32()\n");
      exit(0);
   }
   
   return v-nl;
}

/* free a Word32 vector allocated by svector()              */
/* from numerical recipes in C 2nd edition, page 946        */
void deallocWord32(Word32 *v, long nl, long nh)
{
   
   free((char *)(v+nl));
   
   return;
}
