/*
 * Copyright 2005-2019 by BerryWorks Software, LLC. All rights reserved.
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

package com.berryworks.edireader;

import com.berryworks.edireader.util.BranchingWriter;
import com.berryworks.edireader.util.DateTimeGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.berryworks.edireader.util.FixedLength.valueOf;

/**
 * A delegate for generating an interchange containing some number of 997
 * transactions acknowledging the functional groups parsed by AnsiReader.
 */
public class AnsiFAGenerator extends ReplyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    public static final String REGEX_CHARS_NEEDING_ESCAPE = "\\.[{(*+?^$|";
    public static final String REGEX_PREFIX = "=\\\\";

    protected final BranchingWriter ackStream;
    protected boolean preambleGenerated, skipFA;
    protected String thisInterchangeControlNumber;
    protected String thisGroupControlNumber;
    protected int thisDocumentCount;
    protected String referencedISA;
    protected boolean headerGenerated;
    protected boolean groupTrailerGenerated;
    protected char delimiter;
    protected String terminatorWithSuffix;

    private static final String CONTROL_NUMBER_997 = "0001";

    public AnsiFAGenerator(final StandardReader ansiReader, final BranchingWriter ackStream) {
        this.standardReader = ansiReader;
        this.ackStream = ackStream;
    }

    @Override
    public void generateAcknowledgmentHeader(String firstSegment,
                                             String groupSender, String groupReceiver, int groupDateLength,
                                             String groupVersion, String groupFunctionCode,
                                             String groupControlNumber) throws IOException {
        if (ackStream == null)
            return;

        // Do not generate an FA to acknowledge an FA
        if ("FA".equals(groupFunctionCode)) {
            skipFA = true;
            return;
        }
        skipFA = false;

        logger.debug("generating FA envelope");
        generateAcknowledgementPreamble(firstSegment, groupSender,
                groupReceiver, groupDateLength, groupVersion);

        // Generate the ST 997
        logger.debug("generating first part of 997");
        thisDocumentCount++;
        ackStream.write("ST" + delimiter + "997" + delimiter + CONTROL_NUMBER_997);
        ackStream.write(terminatorWithSuffix);

        // Generate the AK1 segment to identify the group being acknowledged
        ackStream.write("AK1" + delimiter + groupFunctionCode + delimiter
                + groupControlNumber);
        ackStream.write(terminatorWithSuffix);
        headerGenerated = true;
    }

    @Override
    public void generateTransactionAcknowledgment(String transactionCode,
                                                  String controlNumber) throws IOException {
        if (ackStream == null || skipFA || standardReader.isGroupAcknowledgment())
            return;

        generateTransactionAcknowledgmentUsing(transactionCode, controlNumber);
    }

    protected void generateTransactionAcknowledgmentUsing(String transactionCode, String controlNumber) {
        logger.debug("generating AK2/AK5");
        // Generate the AK2 segment to identify the transaction set
        ackStream.writeTrunk("AK2" + delimiter + transactionCode + delimiter
                + controlNumber);
        ackStream.writeTrunk(terminatorWithSuffix);

        // Generate the AK5 segment acknowledging the transaction set
        ackStream.writeTrunk("AK5" + delimiter + "A");
        ackStream.writeTrunk(terminatorWithSuffix);
    }

    @Override
    public void generateGroupAcknowledgmentTrailer(int docCount)
            throws IOException {
        if (ackStream == null || skipFA)
            return;

        logger.debug("generating AK9, SE");
        // For the trunk, generate the AK9 segment to designate acceptance of the entire
        // functional group.
        ackStream.writeTrunk("AK9" + delimiter + "A" + delimiter + docCount
                + delimiter + docCount + delimiter + docCount);
        // For the branch, generate the AK9 segment to designate rejection of the entire
        // functional group.
        ackStream.writeBranch("AK9" + delimiter + "R" + delimiter + docCount
                + delimiter + docCount + delimiter + "0");
        ackStream.write(terminatorWithSuffix);

        // Generate the SE to match the ST
        final int segmentCount = 4 + (standardReader.isGroupAcknowledgment() ? 0 : 2 * docCount);
        ackStream.writeTrunk("SE" + delimiter + segmentCount + delimiter + CONTROL_NUMBER_997);
        ackStream.writeBranch("SE" + delimiter + "4" + delimiter + CONTROL_NUMBER_997);
        ackStream.write(terminatorWithSuffix);
        groupTrailerGenerated = true;
    }

    @Override
    public void generateNegativeACK() throws IOException {
        if (ackStream == null || skipFA || !headerGenerated)
            return;

        logger.debug("recasting 997 as negative");
        if (!groupTrailerGenerated)
            generateGroupAcknowledgmentTrailer(0);
        generateAcknowledgementWrapup(false);
    }

    @Override
    public void generateAcknowledgementWrapup() throws IOException {
        generateAcknowledgementWrapup(true);
    }

    public void generateAcknowledgementWrapup(boolean positiveFA) throws IOException {
        if (ackStream == null || skipFA)
            return;

        logger.debug("generating GE, IEA");
        // Generate the GE to match the GS
        ackStream.write("GE" + delimiter + thisDocumentCount + delimiter + thisGroupControlNumber);
        ackStream.write(terminatorWithSuffix);

        // Finish with an IEA corresponding to the ISA
        ackStream.write("IEA" + delimiter + "1" + delimiter + thisInterchangeControlNumber);
        ackStream.write(terminatorWithSuffix);
        if (positiveFA)
            ackStream.close();
        else
            ackStream.closeUsingBranch();
    }

    protected void generateAcknowledgementPreamble(String firstSegment,
                                                   String groupSender, String groupReceiver, int groupDateLength,
                                                   String groupVersion) throws IOException {
        if (ackStream == null || preambleGenerated)
            return;

        referencedISA = firstSegment;

        // Note that the initialization of the following items cannot occur
        // in the constructor because ansiReader may not have all of the
        // necessary information at that point.
        establishSyntaxCharacters();

        // The ISA envelope is basically the same as that of the input
        // interchange except for reversal of the sender and receiver addresses.
        // Force the generated ISA to have fixed length fields, even if the input ISA does not.

        final String[] isaFields = splitOnDelimiter();
        if (isaFields.length < 17) {
            throw new RuntimeException("*** Internal Error: Unable to interpret input ISA when forming ISA for acknowledgement. " + referencedISA);
        }
        thisInterchangeControlNumber = valueOf(isaFields[13], 9);
        String faHeader = isaFields[0] + delimiter +
                valueOf(isaFields[1], 2) + delimiter +
                valueOf(isaFields[2], 10) + delimiter +
                valueOf(isaFields[3], 2) + delimiter +
                valueOf(isaFields[4], 10) + delimiter +
                valueOf(isaFields[7], 2) + delimiter +
                valueOf(isaFields[8], 15) + delimiter +
                valueOf(isaFields[5], 2) + delimiter +
                valueOf(isaFields[6], 15) + delimiter +
                DateTimeGenerator.generate(delimiter) + delimiter +
                valueOf(isaFields[11], 1) + delimiter +
                valueOf(isaFields[12], 5) + delimiter +
                thisInterchangeControlNumber + delimiter +
                "0" + delimiter +
                valueOf(isaFields[15], 1) + delimiter +
                valueOf(isaFields[16], 1);
        thisInterchangeControlNumber = thisInterchangeControlNumber.trim();

        char senderDelimiter = standardReader.getDelimiter();
        if (senderDelimiter != delimiter)
            faHeader = faHeader.replace(senderDelimiter, delimiter);

        ackStream.write(faHeader);
        ackStream.write(terminatorWithSuffix);

        if (standardReader.isInterchangeAcknowledgment()) {
            ackStream.write("TA1" + delimiter +
                    thisInterchangeControlNumber + delimiter +
                    valueOf(isaFields[9], 6) + delimiter +
                    valueOf(isaFields[10], 4) + delimiter);
            ackStream.writeTrunk("A" + delimiter + "000");
            ackStream.writeBranch("R" + delimiter + "022");
            ackStream.write(terminatorWithSuffix);
        }

        // There are several ways we could select a control number for the GS/GE segments.
        // It needs to be only digits, and no more than 9.
        // It could be a constant, such as "0001". It could be generated from a timestamp.
        // Here, we simply use the control number that came in the ISA of the original interchange.
        thisGroupControlNumber = thisInterchangeControlNumber;

        // Write the GS segment
        ackStream.write("GS" + delimiter + "FA" + delimiter + groupReceiver
                + delimiter + groupSender + delimiter
                + controlDateAndTime(groupDateLength, delimiter) + delimiter
                + thisGroupControlNumber + delimiter + "X" + delimiter
                + groupVersion);
        ackStream.write(terminatorWithSuffix);

        preambleGenerated = true;
    }

    private String[] splitOnDelimiter() {
        final String delimiterAsString = String.valueOf(referencedISA.charAt(3));
        final String delimiterPattern = REGEX_CHARS_NEEDING_ESCAPE.contains(delimiterAsString) ? REGEX_PREFIX + delimiter : delimiterAsString;
        return referencedISA.split(delimiterPattern);
    }

    private void establishSyntaxCharacters() {
        SyntaxDescriptor sd = standardReader.getAcknowledgmentSyntaxDescriptor();
        char terminator = 0;
        String terminatorSuffix = null;
        if (sd == null) {
            delimiter = 0;
        } else {
            delimiter = sd.getDelimiter();
            terminator = sd.getTerminator();
            terminatorSuffix = sd.getTerminatorSuffix();
        }

        if (delimiter == 0)
            delimiter = standardReader.getDelimiter();

        if (terminator == 0)
            terminator = standardReader.getTerminator();

        if (terminatorSuffix == null)
            terminatorSuffix = standardReader.getTerminatorSuffix();

        terminatorWithSuffix = terminator + terminatorSuffix;
    }

    @Override
    public void generateAcknowledgmentHeader(String syntaxIdentifier,
                                             String syntaxVersionNumber, String fromId, String fromQual,
                                             String toId, String toQual, String interchangeControlNumber) {
    }

}
