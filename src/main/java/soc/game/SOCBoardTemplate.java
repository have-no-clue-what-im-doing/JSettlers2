package soc.game;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a fixed classic (4-player, 19-hex) board layout from a plain-text template file,
 * for reproducible bot-only test games via {@link SOCBoard#PROP_JSETTLERS_BOARD_TEMPLATE}.
 *<P>
 * <B>File format:</B> one line per hex, 19 lines total, in board "position" order 0-18
 * (counterclockwise from the southwest hex; see {@link SOCBoard4p#makeNewBoard_numPaths_v1}[0]
 * for the exact index mapping). Blank lines and lines starting with {@code #} are ignored.
 * Each line is:
 *<PRE>
 *   &lt;position&gt; &lt;resource&gt; [&lt;number&gt;]
 *</PRE>
 * where resource is one of: CLAY ORE SHEEP WHEAT WOOD DESERT (case-insensitive).
 * The dice number is required for every resource except DESERT (which gets the robber
 * and no number). Example line: {@code 0 WOOD 5}
 *<P>
 * All 19 positions (0-18) must appear exactly once, and the resource/number counts
 * must match a standard board (1 desert, 3 clay, 3 ore, 4 sheep, 4 wheat, 4 wood;
 * numbers 2,3,3,4,4,5,5,6,6,8,8,9,9,10,10,11,11,12) or {@link #load} throws.
 *
 * @since custom
 */
public class SOCBoardTemplate
{
    /**
     * Load a board template file.
     * @param path  Path to the template file
     * @param hexCount  Expected number of land hexes (19 for the classic 4-player board)
     * @return  a 2-element array: [0] is the resource type for each position 0..hexCount-1
     *          (in template line order), [1] is the dice numbers in the order they should be
     *          consumed for non-desert hexes (in ascending position order)
     * @throws IllegalArgumentException if the file is missing, malformed, or doesn't describe
     *          a complete, standard-resource-count board
     */
    public static int[][] load(final String path, final int hexCount)
    {
        final int[] landHex = new int[hexCount];
        final int[] numberAtPos = new int[hexCount];
        final boolean[] seen = new boolean[hexCount];
        java.util.Arrays.fill(landHex, -1);

        try (BufferedReader r = new BufferedReader(new FileReader(path)))
        {
            String line;
            int lineNum = 0;
            while ((line = r.readLine()) != null)
            {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                final String[] parts = line.split("\\s+");
                if (parts.length < 2)
                    throw new IllegalArgumentException
                        ("Line " + lineNum + ": expected '<position> <resource> [<number>]', got: " + line);

                final int pos;
                try {
                    pos = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Line " + lineNum + ": invalid position: " + parts[0]);
                }
                if ((pos < 0) || (pos >= hexCount))
                    throw new IllegalArgumentException
                        ("Line " + lineNum + ": position " + pos + " out of range 0.." + (hexCount - 1));
                if (seen[pos])
                    throw new IllegalArgumentException("Line " + lineNum + ": position " + pos + " listed twice");
                seen[pos] = true;

                final int resType = resourceFromName(parts[1], lineNum);
                landHex[pos] = resType;

                if (resType == SOCBoard.DESERT_HEX)
                {
                    numberAtPos[pos] = -1;
                    if (parts.length > 2)
                        throw new IllegalArgumentException
                            ("Line " + lineNum + ": DESERT should not have a dice number");
                }
                else
                {
                    if (parts.length < 3)
                        throw new IllegalArgumentException
                            ("Line " + lineNum + ": missing dice number for " + parts[1]);
                    try {
                        numberAtPos[pos] = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Line " + lineNum + ": invalid number: " + parts[2]);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Could not read board template file: " + path, e);
        }

        for (int i = 0; i < hexCount; i++)
            if (! seen[i])
                throw new IllegalArgumentException
                    ("Board template " + path + " is missing position " + i + " (needs all 0.." + (hexCount - 1) + ")");

        // Build the numbers[] array in position order, skipping the desert
        final List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < hexCount; i++)
            if (landHex[i] != SOCBoard.DESERT_HEX)
                numbers.add(numberAtPos[i]);

        final int[] numArr = new int[numbers.size()];
        for (int i = 0; i < numArr.length; i++)
            numArr[i] = numbers.get(i);

        return new int[][] { landHex, numArr };
    }

    private static int resourceFromName(final String name, final int lineNum)
    {
        switch (name.toUpperCase())
        {
        case "CLAY":   return SOCBoard.CLAY_HEX;
        case "ORE":    return SOCBoard.ORE_HEX;
        case "SHEEP":  return SOCBoard.SHEEP_HEX;
        case "WHEAT":  return SOCBoard.WHEAT_HEX;
        case "WOOD":   return SOCBoard.WOOD_HEX;
        case "DESERT": return SOCBoard.DESERT_HEX;
        default:
            throw new IllegalArgumentException
                ("Line " + lineNum + ": unknown resource '" + name + "' (expected CLAY/ORE/SHEEP/WHEAT/WOOD/DESERT)");
        }
    }
}
