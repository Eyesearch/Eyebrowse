/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search.suggestions

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParserTest {

    @Test
    fun `can parse a response from Google`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse for mac\",\"eyebrowse quantum\",\"eyebrowse update\",\"eyebrowse esr\",\"eyebrowse focus\",\"eyebrowse addons\",\"eyebrowse extensions\",\"eyebrowse nightly\",\"eyebrowse clear cache\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse for mac", "eyebrowse quantum", "eyebrowse update", "eyebrowse esr", "eyebrowse focus", "eyebrowse addons", "eyebrowse extensions", "eyebrowse nightly", "eyebrowse clear cache")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Amazon`() {
        val json = "[\"eyebrowse\",[\"eyebrowse for fire tv\",\"eyebrowse movie\",\"eyebrowse app\",\"eyebrowse books\",\"eyebrowse glider\",\"eyebrowse stick\",\"eyebrowse for firestick\",\"eyebrowse\",\"eyebrowse books series\",\"eyebrowse browser\"],[{\"nodes\":[{\"name\":\"Apps & Games\",\"alias\":\"mobile-apps\"}]},{},{},{},{},{},{},{},{},{}],[],\"344I6KZL0KU9N\"]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse for fire tv", "eyebrowse movie", "eyebrowse app", "eyebrowse books", "eyebrowse glider", "eyebrowse stick", "eyebrowse for firestick", "eyebrowse", "eyebrowse books series", "eyebrowse browser")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Azerdict`() {
        val json = "{\"query\":\"code\",\"suggestions\":[\"code\",\"codec\",\"codex\",\"codeine\",\"code of laws\"]}"

        val results = azerdictResponseParser(json)
        val expectedResults = listOf("code", "codec", "codex", "codeine", "code of laws")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Baidu`() {
        val json = "[\"eyebrowse\",[\"eyebrowse rocket\",\"eyebrowse手机浏览器\",\"eyebrowse是什么意思\",\"eyebrowse focus\",\"eyebrowse风哥\",\"eyebrowse官网\",\"eyebrowse吧\",\"eyebrowse os\",\"eyebrowse国际版\",\"eyebrowse android\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse rocket", "eyebrowse手机浏览器", "eyebrowse是什么意思", "eyebrowse focus", "eyebrowse风哥", "eyebrowse官网", "eyebrowse吧", "eyebrowse os", "eyebrowse国际版", "eyebrowse android")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Bing`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse download\",\"eyebrowse for windows 10\",\"eyebrowse browser\",\"eyebrowse quantum\",\"eyebrowse esr\",\"eyebrowse 64-bit\",\"eyebrowse mozilla\",\"eyebrowse nightly\",\"eyebrowse update\",\"eyebrowse install\",\"eyebrowse focus\",\"eyebrowse beta\",\"eyebrowse developer edition\",\"eyebrowse portable\",\"eyebrowse add-ons\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse download", "eyebrowse for windows 10", "eyebrowse browser", "eyebrowse quantum", "eyebrowse esr", "eyebrowse 64-bit", "eyebrowse mozilla", "eyebrowse nightly", "eyebrowse update", "eyebrowse install", "eyebrowse focus", "eyebrowse beta", "eyebrowse developer edition", "eyebrowse portable", "eyebrowse add-ons")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Coccoc`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse tiếng việt\",\"eyebrowse download\",\"eyebrowse quantum\",\"eyebrowse 51\",\"eyebrowse 42\",\"eyebrowse english\",\"eyebrowse portable\",\"eyebrowse 49\",\"eyebrowse viet nam\"],[\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"],[\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"],{\"google:suggesttype\":[\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\",\"QUERY\"]}]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse tiếng việt", "eyebrowse download", "eyebrowse quantum", "eyebrowse 51", "eyebrowse 42", "eyebrowse english", "eyebrowse portable", "eyebrowse 49", "eyebrowse viet nam")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Daum`() {
        val json = "{\"q\":\"eyebrowse\",\"rq\":\"eyebrowse\",\"items\":[\"eyebrowse\",\"eyebrowse download\",\"eyebrowse focus\",\"mozilla eyebrowse\",\"eyebrowse adobe flash\",\"eyebrowse 삭제\",\"eyebrowse 한글판\",\"eyebrowse 즐겨찾기 가져오기\",\"eyebrowse quantum\",\"eyebrowse 57\",\"mozila eyebrowse\",\"mozilla eyebrowse download\",\"mozilla eyebrowse 삭제\",\"Hacking Eyebrowse\",\"Programming Eyebrowse\"],\"r_items\":[]}"

        val results = daumResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse download", "eyebrowse focus", "mozilla eyebrowse", "eyebrowse adobe flash", "eyebrowse 삭제", "eyebrowse 한글판", "eyebrowse 즐겨찾기 가져오기", "eyebrowse quantum", "eyebrowse 57", "mozila eyebrowse", "mozilla eyebrowse download", "mozilla eyebrowse 삭제", "Hacking Eyebrowse", "Programming Eyebrowse")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Duck Duck Go`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse browser\",\"eyebrowse.com\",\"eyebrowse update\",\"eyebrowse for mac\",\"eyebrowse quantum\",\"eyebrowse extensions\",\"eyebrowse esr\",\"eyebrowse clear cache\",\"eyebrowse themes\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse browser", "eyebrowse.com", "eyebrowse update", "eyebrowse for mac", "eyebrowse quantum", "eyebrowse extensions", "eyebrowse esr", "eyebrowse clear cache", "eyebrowse themes")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Naver`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"Mozilla Eyebrowse\",\"eyebrowse add-on to detect vulnerable websites\",\"eyebrowse ak\",\"eyebrowse as gaeilge\",\"eyebrowse developer edition\",\"eyebrowse down\",\"eyebrowse for dummies\",\"eyebrowse for mac\",\"eyebrowse for mobile\"],[],[\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=Mozilla+Eyebrowse\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+add-on+to+detect+vulnerable+websites\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+ak\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+as+gaeilge\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+developer+edition\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+down\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+for+dummies\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+for+mac\",\"http://search.naver.com/search.naver?where=nexearch&sm=osp_sug&ie=utf8&query=eyebrowse+for+mobile\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "Mozilla Eyebrowse", "eyebrowse add-on to detect vulnerable websites", "eyebrowse ak", "eyebrowse as gaeilge", "eyebrowse developer edition", "eyebrowse down", "eyebrowse for dummies", "eyebrowse for mac", "eyebrowse for mobile")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Prisjakt`() {
        val json = "[\"eyebrowse\",[\"Eyebrowse\",\"Eyebrowse\",\"Mountain Equipment Eyebrowse Pants (Herr)\",\"Mountain Equipment Eyebrowse (Herr)\",\"Eyebrowse (DE)\"],[\"L\\u00e4gsta pris: 149:-\",\"L\\u00e4gsta pris: 205:-\",\"L\\u00e4gsta pris: 1 559:-\",\"L\\u00e4gsta pris: 2 773:-\",\"L\\u00e4gsta pris: 198:-\"],[\"https:\\/\\/www.prisjakt.nu\\/produkt.php?p=886794\",\"https:\\/\\/www.prisjakt.nu\\/produkt.php?p=53272\",\"https:\\/\\/www.prisjakt.nu\\/produkt.php?p=3548472\",\"https:\\/\\/www.prisjakt.nu\\/produkt.php?p=1822581\",\"https:\\/\\/www.prisjakt.nu\\/produkt.php?p=3408551\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("Eyebrowse", "Eyebrowse", "Mountain Equipment Eyebrowse Pants (Herr)", "Mountain Equipment Eyebrowse (Herr)", "Eyebrowse (DE)")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Qwant`() {
        val json = "{\"status\":\"success\",\"data\":{\"items\":[{\"value\":\"eyebrowse (video game)\",\"suggestType\":3},{\"value\":\"eyebrowse addons\",\"suggestType\":12},{\"value\":\"eyebrowse\",\"suggestType\":2},{\"value\":\"eyebrowse quantum\",\"suggestType\":12},{\"value\":\"eyebrowse focus\",\"suggestType\":12}],\"special\":[],\"availableQwick\":[]}}"

        val results = qwantResponseParser(json)
        val expectedResults = listOf("eyebrowse (video game)", "eyebrowse addons", "eyebrowse", "eyebrowse quantum", "eyebrowse focus")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Seznam`() {
        val json = "[\"eyebrowse\", [\"eyebrowse\", \"eyebrowse ak keep it up\", \"eyebrowse ak city to city\", \"eyebrowse ak all those people\", \"eyebrowse ak who can act\", \"eyebrowse ak color the trees\", \"eyebrowse ak habibi\", \"eyebrowse ak zodiac\", \"eyebrowse ak the draft\", \"mozilla eyebrowse\"], [], []]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse ak keep it up", "eyebrowse ak city to city", "eyebrowse ak all those people", "eyebrowse ak who can act", "eyebrowse ak color the trees", "eyebrowse ak habibi", "eyebrowse ak zodiac", "eyebrowse ak the draft", "mozilla eyebrowse")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Wikipedia`() {
        val json = "[\"code\",[\"Code\",\"Code Geass\",\"Codeine\",\"Codename: Kids Next Door\",\"Code page\",\"Codex Sinaiticus\",\"Code talker\",\"Code Black (TV series)\",\"Codependency\",\"Codex Vaticanus\"],[\"In communications and information processing, code is a system of rules to convert information\\u2014such as a letter, word, sound, image, or gesture\\u2014into another form or representation, sometimes shortened or secret, for communication through a communication channel or storage in a storage medium.\",\"Code Geass: Lelouch of the Rebellion (\\u30b3\\u30fc\\u30c9\\u30ae\\u30a2\\u30b9 \\u53cd\\u9006\\u306e\\u30eb\\u30eb\\u30fc\\u30b7\\u30e5, K\\u014ddo Giasu: Hangyaku no Rur\\u016bshu), often referred to as simply Code Geass, is a Japanese anime series created by Sunrise, directed by Gor\\u014d Taniguchi, and written by Ichir\\u014d \\u014ckouchi, with original character designs by manga authors Clamp.\",\"Codeine is an opiate used to treat pain, as a cough medicine, and for diarrhea. It is typically used to treat mild to moderate degrees of pain.\",\"Codename: Kids Next Door, commonly abbreviated to Kids Next Door or KND, is an American animated television series created by Tom Warburton for Cartoon Network, and the 13th of the network's Cartoon Cartoons.\",\"In computing, a code page is a table of values that describes the character set used for encoding a particular set of characters, usually combined with a number of control characters.\",\"Codex Sinaiticus (Greek: \\u03a3\\u03b9\\u03bd\\u03b1\\u03ca\\u03c4\\u03b9\\u03ba\\u03cc\\u03c2 \\u039a\\u03ce\\u03b4\\u03b9\\u03ba\\u03b1\\u03c2, Hebrew: \\u05e7\\u05d5\\u05d3\\u05e7\\u05e1 \\u05e1\\u05d9\\u05e0\\u05d0\\u05d9\\u05d8\\u05d9\\u05e7\\u05d5\\u05e1\\u200e; Shelfmarks and references: London, Brit.\",\"Code talkers are people in the 20th century who used obscure languages as a means of secret communication during wartime.\",\"Code Black is an American medical drama television series created by Michael Seitzman which premiered on CBS on September 30, 2015. It takes place in an overcrowded and understaffed emergency room in Los Angeles, California, and is based on a documentary by Ryan McGarry.\",\"Codependency is a controversial concept for a dysfunctional helping relationship where one person supports or enables another person's addiction, poor mental health, immaturity, irresponsibility, or under-achievement.\",\"The Codex Vaticanus (The Vatican, Bibl. Vat., Vat. gr. 1209; no. B or 03 Gregory-Aland, \\u03b4 1 von Soden) is regarded as the oldest extant manuscript of the Greek Bible (Old and New Testament), one of the four great uncial codices.\"],[\"https://en.wikipedia.org/wiki/Code\",\"https://en.wikipedia.org/wiki/Code_Geass\",\"https://en.wikipedia.org/wiki/Codeine\",\"https://en.wikipedia.org/wiki/Codename:_Kids_Next_Door\",\"https://en.wikipedia.org/wiki/Code_page\",\"https://en.wikipedia.org/wiki/Codex_Sinaiticus\",\"https://en.wikipedia.org/wiki/Code_talker\",\"https://en.wikipedia.org/wiki/Code_Black_(TV_series)\",\"https://en.wikipedia.org/wiki/Codependency\",\"https://en.wikipedia.org/wiki/Codex_Vaticanus\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("Code", "Code Geass", "Codeine", "Codename: Kids Next Door", "Code page", "Codex Sinaiticus", "Code talker", "Code Black (TV series)", "Codependency", "Codex Vaticanus")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Yahoo`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse browser\",\"eyebrowse.com\",\"eyebrowse update\"],[],[]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse browser", "eyebrowse.com", "eyebrowse update")
        assertEquals(expectedResults, results)
    }

    @Test
    fun `can parse a response from Yandex`() {
        val json = "[\"eyebrowse\",[\"eyebrowse\",\"eyebrowse download\",\"eyebrowse browser\",\"eyebrowse update\",\"eyebrowse.com\"]]"

        val results = defaultResponseParser(json)
        val expectedResults = listOf("eyebrowse", "eyebrowse download", "eyebrowse browser", "eyebrowse update", "eyebrowse.com")
        assertEquals(expectedResults, results)
    }
}
