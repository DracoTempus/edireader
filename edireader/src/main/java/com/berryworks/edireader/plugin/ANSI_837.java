/*
 * Copyright 2005-2015 by BerryWorks Software, LLC. All rights reserved.
 *
 * This file is part of EDIReader. You may obtain a license for its use directly from
 * BerryWorks Software, and you may also choose to use this software under the terms of the
 * GPL version 3. Other products in the EDIReader software suite are available only by licensing
 * with BerryWorks. Only those files bearing the GPL statement below are available under the GPL.
 *
 * EDIReader is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EDIReader is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EDIReader.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

/*
*  Generated by plugin generator utility by BerryWorks Software, LLC.
*
*  using /home/mayberry/IntelliJProjects/edireader-trunk/testdata/sef/hipaa/837_X098.SEF
*  on Tue Jul 15 22:36:07 CDT 2008
*/
package com.berryworks.edireader.plugin;

import com.berryworks.edireader.Plugin;

public class ANSI_837 extends Plugin
{
  public ANSI_837()
  {
    super("837", "Health Care Claim");
    loops = new LoopDescriptor[]{
      new LoopDescriptor("2300", "CLM", 2, "/2000/2300"),
      new LoopDescriptor("2300", "CLM", 2, "/2000"),
      new LoopDescriptor("2305", "CR7", 3, "/2000/2300/2305"),
      new LoopDescriptor("2305", "CR7", 3, "/2000/2300"),
      new LoopDescriptor("2000", "HL", 1, ANY_CONTEXT),
      new LoopDescriptor("2440", "LQ", 4, "/2000/2300/2400/2440"),
      new LoopDescriptor("2440", "LQ", 4, "/2000/2300/2400"),
      new LoopDescriptor("2400", "LX", 3, "/2000/2300/2400"),
      new LoopDescriptor("2400", "LX", 3, "/2000/2300"),
      new LoopDescriptor("2330", "NM1", 4, "/2000/2300/2320/2330"),
      new LoopDescriptor("2420", "NM1", 4, "/2000/2300/2400/2420"),
      new LoopDescriptor("2310", "NM1", 3, "/2000/2300/2310"),
      new LoopDescriptor("2330", "NM1", 4, "/2000/2300/2320"),
      new LoopDescriptor("2420", "NM1", 4, "/2000/2300/2400"),
      new LoopDescriptor("2010", "NM1", 2, "/2000/2010"),
      new LoopDescriptor("2310", "NM1", 3, "/2000/2300"),
      new LoopDescriptor("2010", "NM1", 2, "/2000"),
      new LoopDescriptor("1000", "NM1", 1, ANY_CONTEXT),
      new LoopDescriptor("2320", "SBR", 3, "/2000/2300/2320"),
      new LoopDescriptor("2320", "SBR", 3, "/2000/2300"),
      new LoopDescriptor(CURRENT, "SBR", 1, "/2000"),
      new LoopDescriptor("2430", "SVD", 4, "/2000/2300/2400/2430"),
      new LoopDescriptor("2430", "SVD", 4, "/2000/2300/2400"),
    };
  }
}
