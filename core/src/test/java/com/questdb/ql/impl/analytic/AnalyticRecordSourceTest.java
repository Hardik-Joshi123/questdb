/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2016 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.ql.impl.analytic;

import com.questdb.JournalEntryWriter;
import com.questdb.JournalWriter;
import com.questdb.factory.configuration.JournalStructure;
import com.questdb.misc.Dates;
import com.questdb.misc.Rnd;
import com.questdb.ql.RecordCursor;
import com.questdb.ql.RecordSource;
import com.questdb.ql.impl.NoOpCancellationHandler;
import com.questdb.ql.parser.AbstractOptimiserTest;
import com.questdb.std.ObjHashSet;
import com.questdb.std.ObjList;
import com.questdb.test.tools.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class AnalyticRecordSourceTest extends AbstractOptimiserTest {

    private final static String expected = "-1148479920\tBZ\t2016-05-01T10:21:00.000Z\t-409854405\n" +
            "1548800833\tKK\t2016-05-01T10:22:00.000Z\t73575701\n" +
            "73575701\tKK\t2016-05-01T10:23:00.000Z\t1326447242\n" +
            "1326447242\tKK\t2016-05-01T10:24:00.000Z\t-1436881714\n" +
            "1868723706\tAX\t2016-05-01T10:25:00.000Z\t-1191262516\n" +
            "-1191262516\tAX\t2016-05-01T10:26:00.000Z\t806715481\n" +
            "-1436881714\tKK\t2016-05-01T10:27:00.000Z\t1530831067\n" +
            "806715481\tAX\t2016-05-01T10:28:00.000Z\t1125579207\n" +
            "1569490116\tXX\t2016-05-01T10:29:00.000Z\t-1532328444\n" +
            "-409854405\tBZ\t2016-05-01T10:30:00.000Z\t1699553881\n" +
            "1530831067\tKK\t2016-05-01T10:31:00.000Z\t-1844391305\n" +
            "-1532328444\tXX\t2016-05-01T10:32:00.000Z\t1404198\n" +
            "1125579207\tAX\t2016-05-01T10:33:00.000Z\t-1432278050\n" +
            "-1432278050\tAX\t2016-05-01T10:34:00.000Z\t-85170055\n" +
            "-85170055\tAX\t2016-05-01T10:35:00.000Z\t-1125169127\n" +
            "-1844391305\tKK\t2016-05-01T10:36:00.000Z\t-1101822104\n" +
            "-1101822104\tKK\t2016-05-01T10:37:00.000Z\t-547127752\n" +
            "1404198\tXX\t2016-05-01T10:38:00.000Z\t1232884790\n" +
            "-1125169127\tAX\t2016-05-01T10:39:00.000Z\t-1975183723\n" +
            "-1975183723\tAX\t2016-05-01T10:40:00.000Z\t-2119387831\n" +
            "1232884790\tXX\t2016-05-01T10:41:00.000Z\t-1575135393\n" +
            "-2119387831\tAX\t2016-05-01T10:42:00.000Z\t1253890363\n" +
            "1699553881\tBZ\t2016-05-01T10:43:00.000Z\t-422941535\n" +
            "1253890363\tAX\t2016-05-01T10:44:00.000Z\t-2132716300\n" +
            "-422941535\tBZ\t2016-05-01T10:45:00.000Z\t-303295973\n" +
            "-547127752\tKK\t2016-05-01T10:46:00.000Z\t-461611463\n" +
            "-303295973\tBZ\t2016-05-01T10:47:00.000Z\t1890602616\n" +
            "-2132716300\tAX\t2016-05-01T10:48:00.000Z\t264240638\n" +
            "-461611463\tKK\t2016-05-01T10:49:00.000Z\t-2144581835\n" +
            "264240638\tAX\t2016-05-01T10:50:00.000Z\t-483853667\n" +
            "-483853667\tAX\t2016-05-01T10:51:00.000Z\t-2002373666\n" +
            "1890602616\tBZ\t2016-05-01T10:52:00.000Z\t68265578\n" +
            "68265578\tBZ\t2016-05-01T10:53:00.000Z\t458818940\n" +
            "-2002373666\tAX\t2016-05-01T10:54:00.000Z\t-1418341054\n" +
            "458818940\tBZ\t2016-05-01T10:55:00.000Z\t-2034804966\n" +
            "-2144581835\tKK\t2016-05-01T10:56:00.000Z\t2031014705\n" +
            "-1418341054\tAX\t2016-05-01T10:57:00.000Z\t-1787109293\n" +
            "2031014705\tKK\t2016-05-01T10:58:00.000Z\t936627841\n" +
            "-1575135393\tXX\t2016-05-01T10:59:00.000Z\t-372268574\n" +
            "936627841\tKK\t2016-05-01T11:00:00.000Z\t-667031149\n" +
            "-667031149\tKK\t2016-05-01T11:01:00.000Z\t1637847416\n" +
            "-2034804966\tBZ\t2016-05-01T11:02:00.000Z\t161592763\n" +
            "1637847416\tKK\t2016-05-01T11:03:00.000Z\t-1819240775\n" +
            "-1819240775\tKK\t2016-05-01T11:04:00.000Z\t-1201923128\n" +
            "-1787109293\tAX\t2016-05-01T11:05:00.000Z\t-1515787781\n" +
            "-1515787781\tAX\t2016-05-01T11:06:00.000Z\t636045524\n" +
            "161592763\tBZ\t2016-05-01T11:07:00.000Z\t-1299391311\n" +
            "636045524\tAX\t2016-05-01T11:08:00.000Z\t-1538602195\n" +
            "-1538602195\tAX\t2016-05-01T11:09:00.000Z\t-443320374\n" +
            "-372268574\tXX\t2016-05-01T11:10:00.000Z\t-10505757\n" +
            "-1299391311\tBZ\t2016-05-01T11:11:00.000Z\t1857212401\n" +
            "-10505757\tXX\t2016-05-01T11:12:00.000Z\t-1566901076\n" +
            "1857212401\tBZ\t2016-05-01T11:13:00.000Z\t1196016669\n" +
            "-443320374\tAX\t2016-05-01T11:14:00.000Z\t1234796102\n" +
            "1196016669\tBZ\t2016-05-01T11:15:00.000Z\t532665695\n" +
            "-1566901076\tXX\t2016-05-01T11:16:00.000Z\t1876812930\n" +
            "-1201923128\tKK\t2016-05-01T11:17:00.000Z\t-1582495445\n" +
            "1876812930\tXX\t2016-05-01T11:18:00.000Z\t-1172180184\n" +
            "-1582495445\tKK\t2016-05-01T11:19:00.000Z\t-45567293\n" +
            "532665695\tBZ\t2016-05-01T11:20:00.000Z\t-373499303\n" +
            "1234796102\tAX\t2016-05-01T11:21:00.000Z\t114747951\n" +
            "-45567293\tKK\t2016-05-01T11:22:00.000Z\t-916132123\n" +
            "-373499303\tBZ\t2016-05-01T11:23:00.000Z\t-1723887671\n" +
            "-916132123\tKK\t2016-05-01T11:24:00.000Z\t-731466113\n" +
            "114747951\tAX\t2016-05-01T11:25:00.000Z\t-1794809330\n" +
            "-1794809330\tAX\t2016-05-01T11:26:00.000Z\t-882371473\n" +
            "-731466113\tKK\t2016-05-01T11:27:00.000Z\t-2075675260\n" +
            "-882371473\tAX\t2016-05-01T11:28:00.000Z\t1235206821\n" +
            "-1723887671\tBZ\t2016-05-01T11:29:00.000Z\t-712702244\n" +
            "-1172180184\tXX\t2016-05-01T11:30:00.000Z\t865832060\n" +
            "-2075675260\tKK\t2016-05-01T11:31:00.000Z\t-1768335227\n" +
            "-712702244\tBZ\t2016-05-01T11:32:00.000Z\t1795359355\n" +
            "-1768335227\tKK\t2016-05-01T11:33:00.000Z\t-1966408995\n" +
            "1235206821\tAX\t2016-05-01T11:34:00.000Z\t838743782\n" +
            "1795359355\tBZ\t2016-05-01T11:35:00.000Z\t-876466531\n" +
            "-876466531\tBZ\t2016-05-01T11:36:00.000Z\t-2043803188\n" +
            "865832060\tXX\t2016-05-01T11:37:00.000Z\t614536941\n" +
            "-1966408995\tKK\t2016-05-01T11:38:00.000Z\t1107889075\n" +
            "838743782\tAX\t2016-05-01T11:39:00.000Z\t-618037497\n" +
            "1107889075\tKK\t2016-05-01T11:40:00.000Z\t-68027832\n" +
            "-618037497\tAX\t2016-05-01T11:41:00.000Z\t519895483\n" +
            "-2043803188\tBZ\t2016-05-01T11:42:00.000Z\t1658228795\n" +
            "-68027832\tKK\t2016-05-01T11:43:00.000Z\t-2088317486\n" +
            "519895483\tAX\t2016-05-01T11:44:00.000Z\t602835017\n" +
            "-2088317486\tKK\t2016-05-01T11:45:00.000Z\t-283321892\n" +
            "602835017\tAX\t2016-05-01T11:46:00.000Z\t-2111250190\n" +
            "-2111250190\tAX\t2016-05-01T11:47:00.000Z\t1598679468\n" +
            "614536941\tXX\t2016-05-01T11:48:00.000Z\t1015055928\n" +
            "1598679468\tAX\t2016-05-01T11:49:00.000Z\t1362833895\n" +
            "1658228795\tBZ\t2016-05-01T11:50:00.000Z\t1238491107\n" +
            "-283321892\tKK\t2016-05-01T11:51:00.000Z\t116799613\n" +
            "116799613\tKK\t2016-05-01T11:52:00.000Z\t-636975106\n" +
            "1238491107\tBZ\t2016-05-01T11:53:00.000Z\t1100812407\n" +
            "-636975106\tKK\t2016-05-01T11:54:00.000Z\t-640305320\n" +
            "1015055928\tXX\t2016-05-01T11:55:00.000Z\tNaN\n" +
            "1100812407\tBZ\t2016-05-01T11:56:00.000Z\t1751526583\n" +
            "1362833895\tAX\t2016-05-01T11:57:00.000Z\t-805434743\n" +
            "-805434743\tAX\t2016-05-01T11:58:00.000Z\tNaN\n" +
            "-640305320\tKK\t2016-05-01T11:59:00.000Z\tNaN\n" +
            "1751526583\tBZ\t2016-05-01T12:00:00.000Z\tNaN\n";

    @BeforeClass
    public static void setUp() throws Exception {
        try (JournalWriter w = factory.bulkWriter(new JournalStructure("xyz")
                .$int("i")
                .$str("str")
                .$ts()
                .$())) {
            int n = 100;
            String[] sym = {"AX", "XX", "BZ", "KK"};
            Rnd rnd = new Rnd();

            long t = Dates.toMillis(2016, 5, 1, 10, 20);
            for (int i = 0; i < n; i++) {
                JournalEntryWriter ew = w.entryWriter(t += 60000);
                ew.putInt(0, rnd.nextInt());
                ew.putStr(1, sym[rnd.nextPositiveInt() % sym.length]);
                ew.append();
            }
            w.commit();
        }


        try (JournalWriter w = factory.bulkWriter(new JournalStructure("abc")
                .$int("i")
                .$double("d")
                .$float("f")
                .$byte("b")
                .$long("l")
                .$str("str")
                .$bool("boo")
                .$sym("sym")
                .$short("sho")
                .$date("date")
                .$ts()
                .$())) {
            int n = 20;
            String[] sym = {"AX", "XX", "BZ", "KK"};
            Rnd rnd = new Rnd();

            long t = Dates.toMillis(2016, 5, 1, 10, 20);
            for (int i = 0; i < n; i++) {
                JournalEntryWriter ew = w.entryWriter(t += 60000);
                ew.putInt(0, rnd.nextInt());
                ew.putDouble(1, rnd.nextDouble());
                ew.putFloat(2, rnd.nextFloat());
                ew.put(3, (byte) rnd.nextInt());
                ew.putLong(4, rnd.nextLong());
                ew.putStr(5, sym[rnd.nextPositiveInt() % sym.length]);
                ew.putBool(6, rnd.nextBoolean());
                ew.putSym(7, sym[rnd.nextPositiveInt() % sym.length]);
                ew.putShort(8, (short) rnd.nextInt());
                ew.putDate(9, rnd.nextLong());
                ew.append();
            }
            w.commit();
        }
    }

    @Test
    public void testBoolean() throws Exception {
        final String expected = "true\tBZ\tBZ\t2016-05-01T10:21:00.000Z\tfalse\n" +
                "false\tXX\tBZ\t2016-05-01T10:22:00.000Z\tfalse\n" +
                "false\tKK\tXX\t2016-05-01T10:23:00.000Z\tfalse\n" +
                "false\tAX\tXX\t2016-05-01T10:24:00.000Z\ttrue\n" +
                "true\tAX\tXX\t2016-05-01T10:25:00.000Z\ttrue\n" +
                "true\tAX\tBZ\t2016-05-01T10:26:00.000Z\tfalse\n" +
                "false\tBZ\tXX\t2016-05-01T10:27:00.000Z\ttrue\n" +
                "true\tBZ\tKK\t2016-05-01T10:28:00.000Z\tfalse\n" +
                "false\tAX\tKK\t2016-05-01T10:29:00.000Z\tfalse\n" +
                "false\tBZ\tAX\t2016-05-01T10:30:00.000Z\tfalse\n" +
                "false\tXX\tKK\t2016-05-01T10:31:00.000Z\ttrue\n" +
                "false\tKK\tAX\t2016-05-01T10:32:00.000Z\tfalse\n" +
                "false\tAX\tAX\t2016-05-01T10:33:00.000Z\ttrue\n" +
                "false\tBZ\tBZ\t2016-05-01T10:34:00.000Z\tfalse\n" +
                "true\tXX\tAX\t2016-05-01T10:35:00.000Z\ttrue\n" +
                "true\tAX\tAX\t2016-05-01T10:36:00.000Z\ttrue\n" +
                "true\tXX\tKK\t2016-05-01T10:37:00.000Z\tfalse\n" +
                "true\tAX\tAX\t2016-05-01T10:38:00.000Z\tfalse\n" +
                "false\tBZ\tBZ\t2016-05-01T10:39:00.000Z\tfalse\n" +
                "false\tBZ\tAX\t2016-05-01T10:40:00.000Z\tfalse\n";
        assertThat(expected, "select boo, str, sym, timestamp , next(boo) over (partition by str) from abc");
    }

    @Test
    public void testByte() throws Exception {
        final String expected = "21\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t53\n" +
                "-120\tXX\tBZ\t2016-05-01T10:22:00.000Z\t-126\n" +
                "-34\tKK\tXX\t2016-05-01T10:23:00.000Z\t-114\n" +
                "-60\tAX\tXX\t2016-05-01T10:24:00.000Z\t-95\n" +
                "-95\tAX\tXX\t2016-05-01T10:25:00.000Z\t-40\n" +
                "-40\tAX\tBZ\t2016-05-01T10:26:00.000Z\t-69\n" +
                "53\tBZ\tXX\t2016-05-01T10:27:00.000Z\t77\n" +
                "77\tBZ\tKK\t2016-05-01T10:28:00.000Z\t-15\n" +
                "-69\tAX\tKK\t2016-05-01T10:29:00.000Z\t-55\n" +
                "-15\tBZ\tAX\t2016-05-01T10:30:00.000Z\t-72\n" +
                "-126\tXX\tKK\t2016-05-01T10:31:00.000Z\t-77\n" +
                "-114\tKK\tAX\t2016-05-01T10:32:00.000Z\t0\n" +
                "-55\tAX\tAX\t2016-05-01T10:33:00.000Z\t-83\n" +
                "-72\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t-114\n" +
                "-77\tXX\tAX\t2016-05-01T10:35:00.000Z\t-36\n" +
                "-83\tAX\tAX\t2016-05-01T10:36:00.000Z\t-102\n" +
                "-36\tXX\tKK\t2016-05-01T10:37:00.000Z\t0\n" +
                "-102\tAX\tAX\t2016-05-01T10:38:00.000Z\t0\n" +
                "-114\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t70\n" +
                "70\tBZ\tAX\t2016-05-01T10:40:00.000Z\t0\n";
        assertThat(expected, "select b, str, sym, timestamp , next(b) over (partition by str) from abc");
    }

    @Test
    public void testCompilation() throws Exception {
        assertThat(expected, "select i, str, timestamp, next(i) over (partition by str) from xyz");
    }

    @Test
    public void testCompileNonPart() throws Exception {
        final String expected = "-1148479920\tBZ\t2016-05-01T10:21:00.000Z\t1548800833\n" +
                "1548800833\tKK\t2016-05-01T10:22:00.000Z\t73575701\n" +
                "73575701\tKK\t2016-05-01T10:23:00.000Z\t1326447242\n" +
                "1326447242\tKK\t2016-05-01T10:24:00.000Z\t1868723706\n" +
                "1868723706\tAX\t2016-05-01T10:25:00.000Z\t-1191262516\n" +
                "-1191262516\tAX\t2016-05-01T10:26:00.000Z\t-1436881714\n" +
                "-1436881714\tKK\t2016-05-01T10:27:00.000Z\t806715481\n" +
                "806715481\tAX\t2016-05-01T10:28:00.000Z\t1569490116\n" +
                "1569490116\tXX\t2016-05-01T10:29:00.000Z\t-409854405\n" +
                "-409854405\tBZ\t2016-05-01T10:30:00.000Z\t1530831067\n" +
                "1530831067\tKK\t2016-05-01T10:31:00.000Z\t-1532328444\n" +
                "-1532328444\tXX\t2016-05-01T10:32:00.000Z\t1125579207\n" +
                "1125579207\tAX\t2016-05-01T10:33:00.000Z\t-1432278050\n" +
                "-1432278050\tAX\t2016-05-01T10:34:00.000Z\t-85170055\n" +
                "-85170055\tAX\t2016-05-01T10:35:00.000Z\t-1844391305\n" +
                "-1844391305\tKK\t2016-05-01T10:36:00.000Z\t-1101822104\n" +
                "-1101822104\tKK\t2016-05-01T10:37:00.000Z\t1404198\n" +
                "1404198\tXX\t2016-05-01T10:38:00.000Z\t-1125169127\n" +
                "-1125169127\tAX\t2016-05-01T10:39:00.000Z\t-1975183723\n" +
                "-1975183723\tAX\t2016-05-01T10:40:00.000Z\t1232884790\n" +
                "1232884790\tXX\t2016-05-01T10:41:00.000Z\t-2119387831\n" +
                "-2119387831\tAX\t2016-05-01T10:42:00.000Z\t1699553881\n" +
                "1699553881\tBZ\t2016-05-01T10:43:00.000Z\t1253890363\n" +
                "1253890363\tAX\t2016-05-01T10:44:00.000Z\t-422941535\n" +
                "-422941535\tBZ\t2016-05-01T10:45:00.000Z\t-547127752\n" +
                "-547127752\tKK\t2016-05-01T10:46:00.000Z\t-303295973\n" +
                "-303295973\tBZ\t2016-05-01T10:47:00.000Z\t-2132716300\n" +
                "-2132716300\tAX\t2016-05-01T10:48:00.000Z\t-461611463\n" +
                "-461611463\tKK\t2016-05-01T10:49:00.000Z\t264240638\n" +
                "264240638\tAX\t2016-05-01T10:50:00.000Z\t-483853667\n" +
                "-483853667\tAX\t2016-05-01T10:51:00.000Z\t1890602616\n" +
                "1890602616\tBZ\t2016-05-01T10:52:00.000Z\t68265578\n" +
                "68265578\tBZ\t2016-05-01T10:53:00.000Z\t-2002373666\n" +
                "-2002373666\tAX\t2016-05-01T10:54:00.000Z\t458818940\n" +
                "458818940\tBZ\t2016-05-01T10:55:00.000Z\t-2144581835\n" +
                "-2144581835\tKK\t2016-05-01T10:56:00.000Z\t-1418341054\n" +
                "-1418341054\tAX\t2016-05-01T10:57:00.000Z\t2031014705\n" +
                "2031014705\tKK\t2016-05-01T10:58:00.000Z\t-1575135393\n" +
                "-1575135393\tXX\t2016-05-01T10:59:00.000Z\t936627841\n" +
                "936627841\tKK\t2016-05-01T11:00:00.000Z\t-667031149\n" +
                "-667031149\tKK\t2016-05-01T11:01:00.000Z\t-2034804966\n" +
                "-2034804966\tBZ\t2016-05-01T11:02:00.000Z\t1637847416\n" +
                "1637847416\tKK\t2016-05-01T11:03:00.000Z\t-1819240775\n" +
                "-1819240775\tKK\t2016-05-01T11:04:00.000Z\t-1787109293\n" +
                "-1787109293\tAX\t2016-05-01T11:05:00.000Z\t-1515787781\n" +
                "-1515787781\tAX\t2016-05-01T11:06:00.000Z\t161592763\n" +
                "161592763\tBZ\t2016-05-01T11:07:00.000Z\t636045524\n" +
                "636045524\tAX\t2016-05-01T11:08:00.000Z\t-1538602195\n" +
                "-1538602195\tAX\t2016-05-01T11:09:00.000Z\t-372268574\n" +
                "-372268574\tXX\t2016-05-01T11:10:00.000Z\t-1299391311\n" +
                "-1299391311\tBZ\t2016-05-01T11:11:00.000Z\t-10505757\n" +
                "-10505757\tXX\t2016-05-01T11:12:00.000Z\t1857212401\n" +
                "1857212401\tBZ\t2016-05-01T11:13:00.000Z\t-443320374\n" +
                "-443320374\tAX\t2016-05-01T11:14:00.000Z\t1196016669\n" +
                "1196016669\tBZ\t2016-05-01T11:15:00.000Z\t-1566901076\n" +
                "-1566901076\tXX\t2016-05-01T11:16:00.000Z\t-1201923128\n" +
                "-1201923128\tKK\t2016-05-01T11:17:00.000Z\t1876812930\n" +
                "1876812930\tXX\t2016-05-01T11:18:00.000Z\t-1582495445\n" +
                "-1582495445\tKK\t2016-05-01T11:19:00.000Z\t532665695\n" +
                "532665695\tBZ\t2016-05-01T11:20:00.000Z\t1234796102\n" +
                "1234796102\tAX\t2016-05-01T11:21:00.000Z\t-45567293\n" +
                "-45567293\tKK\t2016-05-01T11:22:00.000Z\t-373499303\n" +
                "-373499303\tBZ\t2016-05-01T11:23:00.000Z\t-916132123\n" +
                "-916132123\tKK\t2016-05-01T11:24:00.000Z\t114747951\n" +
                "114747951\tAX\t2016-05-01T11:25:00.000Z\t-1794809330\n" +
                "-1794809330\tAX\t2016-05-01T11:26:00.000Z\t-731466113\n" +
                "-731466113\tKK\t2016-05-01T11:27:00.000Z\t-882371473\n" +
                "-882371473\tAX\t2016-05-01T11:28:00.000Z\t-1723887671\n" +
                "-1723887671\tBZ\t2016-05-01T11:29:00.000Z\t-1172180184\n" +
                "-1172180184\tXX\t2016-05-01T11:30:00.000Z\t-2075675260\n" +
                "-2075675260\tKK\t2016-05-01T11:31:00.000Z\t-712702244\n" +
                "-712702244\tBZ\t2016-05-01T11:32:00.000Z\t-1768335227\n" +
                "-1768335227\tKK\t2016-05-01T11:33:00.000Z\t1235206821\n" +
                "1235206821\tAX\t2016-05-01T11:34:00.000Z\t1795359355\n" +
                "1795359355\tBZ\t2016-05-01T11:35:00.000Z\t-876466531\n" +
                "-876466531\tBZ\t2016-05-01T11:36:00.000Z\t865832060\n" +
                "865832060\tXX\t2016-05-01T11:37:00.000Z\t-1966408995\n" +
                "-1966408995\tKK\t2016-05-01T11:38:00.000Z\t838743782\n" +
                "838743782\tAX\t2016-05-01T11:39:00.000Z\t1107889075\n" +
                "1107889075\tKK\t2016-05-01T11:40:00.000Z\t-618037497\n" +
                "-618037497\tAX\t2016-05-01T11:41:00.000Z\t-2043803188\n" +
                "-2043803188\tBZ\t2016-05-01T11:42:00.000Z\t-68027832\n" +
                "-68027832\tKK\t2016-05-01T11:43:00.000Z\t519895483\n" +
                "519895483\tAX\t2016-05-01T11:44:00.000Z\t-2088317486\n" +
                "-2088317486\tKK\t2016-05-01T11:45:00.000Z\t602835017\n" +
                "602835017\tAX\t2016-05-01T11:46:00.000Z\t-2111250190\n" +
                "-2111250190\tAX\t2016-05-01T11:47:00.000Z\t614536941\n" +
                "614536941\tXX\t2016-05-01T11:48:00.000Z\t1598679468\n" +
                "1598679468\tAX\t2016-05-01T11:49:00.000Z\t1658228795\n" +
                "1658228795\tBZ\t2016-05-01T11:50:00.000Z\t-283321892\n" +
                "-283321892\tKK\t2016-05-01T11:51:00.000Z\t116799613\n" +
                "116799613\tKK\t2016-05-01T11:52:00.000Z\t1238491107\n" +
                "1238491107\tBZ\t2016-05-01T11:53:00.000Z\t-636975106\n" +
                "-636975106\tKK\t2016-05-01T11:54:00.000Z\t1015055928\n" +
                "1015055928\tXX\t2016-05-01T11:55:00.000Z\t1100812407\n" +
                "1100812407\tBZ\t2016-05-01T11:56:00.000Z\t1362833895\n" +
                "1362833895\tAX\t2016-05-01T11:57:00.000Z\t-805434743\n" +
                "-805434743\tAX\t2016-05-01T11:58:00.000Z\t-640305320\n" +
                "-640305320\tKK\t2016-05-01T11:59:00.000Z\t1751526583\n" +
                "1751526583\tBZ\t2016-05-01T12:00:00.000Z\tNaN\n";
        assertThat(expected, "select i, str, timestamp, next(i) over () from xyz");
    }

    @Test
    public void testDate() throws Exception {
        final String expected = "277427992-01-2080342T08:51:21.932Z\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t231474144-12-1735467T12:18:04.767Z\n" +
                "000-284204894-12-0-2131416T06:37:25.107Z\tXX\tBZ\t2016-05-01T10:22:00.000Z\t000-201710735-12-0-1512581T10:14:48.134Z\n" +
                "000-142142802-12-0-1065746T10:07:22.984Z\tKK\tXX\t2016-05-01T10:23:00.000Z\t132056646-12-989842T08:27:45.836Z\n" +
                "000-268690388-12-0-2015078T00:06:37.492Z\tAX\tXX\t2016-05-01T10:24:00.000Z\t217212983-05-1628908T18:39:59.220Z\n" +
                "217212983-05-1628908T18:39:59.220Z\tAX\tXX\t2016-05-01T10:25:00.000Z\t263834991-01-1978681T18:15:05.778Z\n" +
                "263834991-01-1978681T18:15:05.778Z\tAX\tBZ\t2016-05-01T10:26:00.000Z\t000-230262069-12-0-1726604T22:57:03.970Z\n" +
                "231474144-12-1735467T12:18:04.767Z\tBZ\tXX\t2016-05-01T10:27:00.000Z\t220348391-12-1652040T00:08:45.841Z\n" +
                "220348391-12-1652040T00:08:45.841Z\tBZ\tKK\t2016-05-01T10:28:00.000Z\t18125162-01-135753T04:06:38.086Z\n" +
                "000-230262069-12-0-1726604T22:57:03.970Z\tAX\tKK\t2016-05-01T10:29:00.000Z\t000-253356772-01-0-1899589T02:07:37.180Z\n" +
                "18125162-01-135753T04:06:38.086Z\tBZ\tAX\t2016-05-01T10:30:00.000Z\t000-180943699-12-0-1356718T10:19:58.733Z\n" +
                "000-201710735-12-0-1512581T10:14:48.134Z\tXX\tKK\t2016-05-01T10:31:00.000Z\t000-225081705-01-0-1687457T21:53:40.936Z\n" +
                "132056646-12-989842T08:27:45.836Z\tKK\tAX\t2016-05-01T10:32:00.000Z\t\n" +
                "000-253356772-01-0-1899589T02:07:37.180Z\tAX\tAX\t2016-05-01T10:33:00.000Z\t252672790-12-1894671T03:29:33.753Z\n" +
                "000-180943699-12-0-1356718T10:19:58.733Z\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t167787767-01-1258238T07:52:02.538Z\n" +
                "000-225081705-01-0-1687457T21:53:40.936Z\tXX\tAX\t2016-05-01T10:35:00.000Z\t000-244299855-12-0-1831941T17:50:45.758Z\n" +
                "252672790-12-1894671T03:29:33.753Z\tAX\tAX\t2016-05-01T10:36:00.000Z\t000-196064580-12-0-1470233T05:32:43.747Z\n" +
                "000-244299855-12-0-1831941T17:50:45.758Z\tXX\tKK\t2016-05-01T10:37:00.000Z\t\n" +
                "000-196064580-12-0-1470233T05:32:43.747Z\tAX\tAX\t2016-05-01T10:38:00.000Z\t\n" +
                "167787767-01-1258238T07:52:02.538Z\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t000-56976438-01-0-426674T13:10:29.515Z\n" +
                "000-56976438-01-0-426674T13:10:29.515Z\tBZ\tAX\t2016-05-01T10:40:00.000Z\t\n";
        assertThat(expected, "select date, str, sym, timestamp , next(date) over (partition by str) from abc");
    }

    @Test
    public void testDouble() throws Exception {
        final String expected = "1.050231933594\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t0.332301996648\n" +
                "566.734375000000\tXX\tBZ\t2016-05-01T10:22:00.000Z\t0.000002473130\n" +
                "0.000013792171\tKK\tXX\t2016-05-01T10:23:00.000Z\t632.921875000000\n" +
                "0.000000567185\tAX\tXX\t2016-05-01T10:24:00.000Z\t-512.000000000000\n" +
                "-512.000000000000\tAX\tXX\t2016-05-01T10:25:00.000Z\t0.675451681018\n" +
                "0.675451681018\tAX\tBZ\t2016-05-01T10:26:00.000Z\t0.000076281818\n" +
                "0.332301996648\tBZ\tXX\t2016-05-01T10:27:00.000Z\t0.000001752813\n" +
                "0.000001752813\tBZ\tKK\t2016-05-01T10:28:00.000Z\t0.000000005555\n" +
                "0.000076281818\tAX\tKK\t2016-05-01T10:29:00.000Z\t0.000000020896\n" +
                "0.000000005555\tBZ\tAX\t2016-05-01T10:30:00.000Z\t0.007371325744\n" +
                "0.000002473130\tXX\tKK\t2016-05-01T10:31:00.000Z\t0.000000014643\n" +
                "632.921875000000\tKK\tAX\t2016-05-01T10:32:00.000Z\tNaN\n" +
                "0.000000020896\tAX\tAX\t2016-05-01T10:33:00.000Z\t512.000000000000\n" +
                "0.007371325744\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t-842.000000000000\n" +
                "0.000000014643\tXX\tAX\t2016-05-01T10:35:00.000Z\t864.000000000000\n" +
                "512.000000000000\tAX\tAX\t2016-05-01T10:36:00.000Z\t0.000000157437\n" +
                "864.000000000000\tXX\tKK\t2016-05-01T10:37:00.000Z\tNaN\n" +
                "0.000000157437\tAX\tAX\t2016-05-01T10:38:00.000Z\tNaN\n" +
                "-842.000000000000\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t0.000032060649\n" +
                "0.000032060649\tBZ\tAX\t2016-05-01T10:40:00.000Z\tNaN\n";
        assertThat(expected, "select d, str, sym, timestamp , next(d) over (partition by str) from abc");
    }

    @Test
    public void testFloat() throws Exception {
        final String expected = "0.6235\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t0.5725\n" +
                "0.7780\tXX\tBZ\t2016-05-01T10:22:00.000Z\t0.7274\n" +
                "0.5509\tKK\tXX\t2016-05-01T10:23:00.000Z\t0.5619\n" +
                "0.0204\tAX\tXX\t2016-05-01T10:24:00.000Z\t0.4848\n" +
                "0.4848\tAX\tXX\t2016-05-01T10:25:00.000Z\t0.2969\n" +
                "0.2969\tAX\tBZ\t2016-05-01T10:26:00.000Z\t0.1609\n" +
                "0.5725\tBZ\tXX\t2016-05-01T10:27:00.000Z\t0.5967\n" +
                "0.5967\tBZ\tKK\t2016-05-01T10:28:00.000Z\t0.3509\n" +
                "0.1609\tAX\tKK\t2016-05-01T10:29:00.000Z\t0.5433\n" +
                "0.3509\tBZ\tAX\t2016-05-01T10:30:00.000Z\t0.5442\n" +
                "0.7274\tXX\tKK\t2016-05-01T10:31:00.000Z\t0.6746\n" +
                "0.5619\tKK\tAX\t2016-05-01T10:32:00.000Z\tNaN\n" +
                "0.5433\tAX\tAX\t2016-05-01T10:33:00.000Z\t0.8217\n" +
                "0.5442\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t0.1168\n" +
                "0.6746\tXX\tAX\t2016-05-01T10:35:00.000Z\t0.3591\n" +
                "0.8217\tAX\tAX\t2016-05-01T10:36:00.000Z\t0.6827\n" +
                "0.3591\tXX\tKK\t2016-05-01T10:37:00.000Z\tNaN\n" +
                "0.6827\tAX\tAX\t2016-05-01T10:38:00.000Z\tNaN\n" +
                "0.1168\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t0.4967\n" +
                "0.4967\tBZ\tAX\t2016-05-01T10:40:00.000Z\tNaN\n";
        assertThat(expected, "select f, str, sym, timestamp , next(f) over (partition by str) from abc");
    }

    @Test
    public void testLong() throws Exception {
        final String expected = "8920866532787660373\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t-6253307669002054137\n" +
                "-6943924477733600060\tXX\tBZ\t2016-05-01T10:22:00.000Z\t-7387846268299105911\n" +
                "-6856503215590263904\tKK\tXX\t2016-05-01T10:23:00.000Z\t7122109662042058469\n" +
                "8416773233910814357\tAX\tXX\t2016-05-01T10:24:00.000Z\t7199909180655756830\n" +
                "7199909180655756830\tAX\tXX\t2016-05-01T10:25:00.000Z\t6270672455202306717\n" +
                "6270672455202306717\tAX\tBZ\t2016-05-01T10:26:00.000Z\t-7316123607359392486\n" +
                "-6253307669002054137\tBZ\tXX\t2016-05-01T10:27:00.000Z\t7392877322819819290\n" +
                "7392877322819819290\tBZ\tKK\t2016-05-01T10:28:00.000Z\t-3107239868490395663\n" +
                "-7316123607359392486\tAX\tKK\t2016-05-01T10:29:00.000Z\t-6626590012581323602\n" +
                "-3107239868490395663\tBZ\tAX\t2016-05-01T10:30:00.000Z\t8611582118025429627\n" +
                "-7387846268299105911\tXX\tKK\t2016-05-01T10:31:00.000Z\t-8082754367165748693\n" +
                "7122109662042058469\tKK\tAX\t2016-05-01T10:32:00.000Z\tNaN\n" +
                "-6626590012581323602\tAX\tAX\t2016-05-01T10:33:00.000Z\t6574958665733670985\n" +
                "8611582118025429627\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t8152044974329490473\n" +
                "-8082754367165748693\tXX\tAX\t2016-05-01T10:35:00.000Z\t3446015290144635451\n" +
                "6574958665733670985\tAX\tAX\t2016-05-01T10:36:00.000Z\t8889492928577876455\n" +
                "3446015290144635451\tXX\tKK\t2016-05-01T10:37:00.000Z\tNaN\n" +
                "8889492928577876455\tAX\tAX\t2016-05-01T10:38:00.000Z\tNaN\n" +
                "8152044974329490473\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t-6071768268784020226\n" +
                "-6071768268784020226\tBZ\tAX\t2016-05-01T10:40:00.000Z\tNaN\n";
        assertThat(expected, "select l, str, sym, timestamp , next(l) over (partition by str) from abc");
    }

    @Test
    public void testShort() throws Exception {
        final String expected = "-19496\tBZ\tBZ\t2016-05-01T10:21:00.000Z\t-391\n" +
                "-24357\tXX\tBZ\t2016-05-01T10:22:00.000Z\t-4874\n" +
                "21781\tKK\tXX\t2016-05-01T10:23:00.000Z\t25102\n" +
                "-19127\tAX\tXX\t2016-05-01T10:24:00.000Z\t-15458\n" +
                "-15458\tAX\tXX\t2016-05-01T10:25:00.000Z\t-22934\n" +
                "-22934\tAX\tBZ\t2016-05-01T10:26:00.000Z\t-19136\n" +
                "-391\tBZ\tXX\t2016-05-01T10:27:00.000Z\t-26951\n" +
                "-26951\tBZ\tKK\t2016-05-01T10:28:00.000Z\t-15331\n" +
                "-19136\tAX\tKK\t2016-05-01T10:29:00.000Z\t-20409\n" +
                "-15331\tBZ\tAX\t2016-05-01T10:30:00.000Z\t-29572\n" +
                "-4874\tXX\tKK\t2016-05-01T10:31:00.000Z\t25974\n" +
                "25102\tKK\tAX\t2016-05-01T10:32:00.000Z\t0\n" +
                "-20409\tAX\tAX\t2016-05-01T10:33:00.000Z\t5869\n" +
                "-29572\tBZ\tBZ\t2016-05-01T10:34:00.000Z\t11755\n" +
                "25974\tXX\tAX\t2016-05-01T10:35:00.000Z\t-22894\n" +
                "5869\tAX\tAX\t2016-05-01T10:36:00.000Z\t-18600\n" +
                "-22894\tXX\tKK\t2016-05-01T10:37:00.000Z\t0\n" +
                "-18600\tAX\tAX\t2016-05-01T10:38:00.000Z\t0\n" +
                "11755\tBZ\tBZ\t2016-05-01T10:39:00.000Z\t-24455\n" +
                "-24455\tBZ\tAX\t2016-05-01T10:40:00.000Z\t0\n";
        assertThat(expected, "select sho, str, sym, timestamp , next(sho) over (partition by str) from abc");
    }

    @Test
    public void testSimple() throws Exception {
        final RecordSource recordSource = compiler.compileSource(factory, "xyz");
        sink.clear();

        final AnalyticRecordSource as = new AnalyticRecordSource(1024 * 1024, recordSource, new ObjList<AnalyticFunction>() {{
            add(new NextRowAnalyticFunction(1024 * 1024, recordSource.getMetadata(), new ObjHashSet<String>() {{
                add("str");
            }}, "i"));
        }});

        sink.clear();
        RecordCursor cursor = as.prepareCursor(factory, NoOpCancellationHandler.INSTANCE);
        printer.printCursor(cursor);

        TestUtils.assertEquals(expected, sink);
    }

    @Test
    public void testStr() throws Exception {
        final String expected = "BZ\tBZ\t2016-05-01T10:21:00.000Z\tXX\n" +
                "XX\tBZ\t2016-05-01T10:22:00.000Z\tAX\n" +
                "KK\tXX\t2016-05-01T10:23:00.000Z\tAX\n" +
                "AX\tXX\t2016-05-01T10:24:00.000Z\tAX\n" +
                "AX\tXX\t2016-05-01T10:25:00.000Z\tBZ\n" +
                "AX\tBZ\t2016-05-01T10:26:00.000Z\tBZ\n" +
                "BZ\tXX\t2016-05-01T10:27:00.000Z\t\n" +
                "BZ\tKK\t2016-05-01T10:28:00.000Z\tAX\n" +
                "AX\tKK\t2016-05-01T10:29:00.000Z\tXX\n" +
                "BZ\tAX\t2016-05-01T10:30:00.000Z\tKK\n" +
                "XX\tKK\t2016-05-01T10:31:00.000Z\tXX\n" +
                "KK\tAX\t2016-05-01T10:32:00.000Z\tAX\n" +
                "AX\tAX\t2016-05-01T10:33:00.000Z\tXX\n" +
                "BZ\tBZ\t2016-05-01T10:34:00.000Z\tBZ\n" +
                "XX\tAX\t2016-05-01T10:35:00.000Z\tAX\n" +
                "AX\tAX\t2016-05-01T10:36:00.000Z\tAX\n" +
                "XX\tKK\t2016-05-01T10:37:00.000Z\t\n" +
                "AX\tAX\t2016-05-01T10:38:00.000Z\tBZ\n" +
                "BZ\tBZ\t2016-05-01T10:39:00.000Z\t\n" +
                "BZ\tAX\t2016-05-01T10:40:00.000Z\t\n";
        assertThat(expected, "select str, sym, timestamp , next(str) over (partition by sym) from abc");
    }

    @Test
    public void testSymNonPart() throws Exception {
        final String expected = "BZ\tBZ\t2016-05-01T10:21:00.000Z\tBZ\n" +
                "XX\tBZ\t2016-05-01T10:22:00.000Z\tXX\n" +
                "KK\tXX\t2016-05-01T10:23:00.000Z\tXX\n" +
                "AX\tXX\t2016-05-01T10:24:00.000Z\tXX\n" +
                "AX\tXX\t2016-05-01T10:25:00.000Z\tBZ\n" +
                "AX\tBZ\t2016-05-01T10:26:00.000Z\tXX\n" +
                "BZ\tXX\t2016-05-01T10:27:00.000Z\tKK\n" +
                "BZ\tKK\t2016-05-01T10:28:00.000Z\tKK\n" +
                "AX\tKK\t2016-05-01T10:29:00.000Z\tAX\n" +
                "BZ\tAX\t2016-05-01T10:30:00.000Z\tKK\n" +
                "XX\tKK\t2016-05-01T10:31:00.000Z\tAX\n" +
                "KK\tAX\t2016-05-01T10:32:00.000Z\tAX\n" +
                "AX\tAX\t2016-05-01T10:33:00.000Z\tBZ\n" +
                "BZ\tBZ\t2016-05-01T10:34:00.000Z\tAX\n" +
                "XX\tAX\t2016-05-01T10:35:00.000Z\tAX\n" +
                "AX\tAX\t2016-05-01T10:36:00.000Z\tKK\n" +
                "XX\tKK\t2016-05-01T10:37:00.000Z\tAX\n" +
                "AX\tAX\t2016-05-01T10:38:00.000Z\tBZ\n" +
                "BZ\tBZ\t2016-05-01T10:39:00.000Z\tAX\n" +
                "BZ\tAX\t2016-05-01T10:40:00.000Z\tnull\n";
        assertThat(expected, "select str, sym, timestamp , next(sym) over () from abc");
    }

    @Test
    public void testSymbol() throws Exception {
        final String expected = "BZ\tBZ\t2016-05-01T10:21:00.000Z\tXX\n" +
                "XX\tBZ\t2016-05-01T10:22:00.000Z\tKK\n" +
                "KK\tXX\t2016-05-01T10:23:00.000Z\tAX\n" +
                "AX\tXX\t2016-05-01T10:24:00.000Z\tXX\n" +
                "AX\tXX\t2016-05-01T10:25:00.000Z\tBZ\n" +
                "AX\tBZ\t2016-05-01T10:26:00.000Z\tKK\n" +
                "BZ\tXX\t2016-05-01T10:27:00.000Z\tKK\n" +
                "BZ\tKK\t2016-05-01T10:28:00.000Z\tAX\n" +
                "AX\tKK\t2016-05-01T10:29:00.000Z\tAX\n" +
                "BZ\tAX\t2016-05-01T10:30:00.000Z\tBZ\n" +
                "XX\tKK\t2016-05-01T10:31:00.000Z\tAX\n" +
                "KK\tAX\t2016-05-01T10:32:00.000Z\tnull\n" +
                "AX\tAX\t2016-05-01T10:33:00.000Z\tAX\n" +
                "BZ\tBZ\t2016-05-01T10:34:00.000Z\tBZ\n" +
                "XX\tAX\t2016-05-01T10:35:00.000Z\tKK\n" +
                "AX\tAX\t2016-05-01T10:36:00.000Z\tAX\n" +
                "XX\tKK\t2016-05-01T10:37:00.000Z\tnull\n" +
                "AX\tAX\t2016-05-01T10:38:00.000Z\tnull\n" +
                "BZ\tBZ\t2016-05-01T10:39:00.000Z\tAX\n" +
                "BZ\tAX\t2016-05-01T10:40:00.000Z\tnull\n";
        assertThat(expected, "select str, sym, timestamp , next(sym) over (partition by str) from abc");
    }
}