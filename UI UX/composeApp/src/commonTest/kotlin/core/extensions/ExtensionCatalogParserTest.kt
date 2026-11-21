package core.extensions

import core.extensions.js.ExtensionType
import core.extensions.model.ExtensionEcosystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionCatalogParserTest {

    private val parser = ExtensionCatalogParser()

    @Test
    fun testParseMangayomiCatalog() {
        val json = """
            [
              {
                "id": 1001,
                "name": "Asura Scans",
                "baseUrl": "https://asuracomic.net",
                "lang": "en",
                "version": "1.0.4",
                "sourceCodeUrl": "https://raw.githubusercontent.com/kodjodevf/mangayomi-extensions/main/javascript/asura.js",
                "sourceCodeLanguage": 1,
                "itemType": 0,
                "iconUrl": "https://asuracomic.net/favicon.ico",
                "isNsfw": false
              },
              {
                "id": 1002,
                "name": "Dart Manga (Should be ignored)",
                "baseUrl": "https://dart.example.com",
                "lang": "en",
                "version": "1.0.0",
                "sourceCodeUrl": "https://dart.example.com/source.dart",
                "sourceCodeLanguage": 0,
                "itemType": 0,
                "iconUrl": "",
                "isNsfw": false
              },
              {
                "id": 1003,
                "name": "KolNovel",
                "baseUrl": "https://kolnovel.com",
                "lang": "ar",
                "version": "0.1.2",
                "sourceCodeUrl": "https://raw.githubusercontent.com/kodjodevf/mangayomi-extensions/main/javascript/kolnovel.js",
                "sourceCodeLanguage": 1,
                "itemType": 2,
                "iconUrl": "",
                "isNsfw": false
              }
            ]
        """.trimIndent()

        val items = parser.parseCatalog(json, "https://raw.githubusercontent.com/kodjodevf/mangayomi-extensions/main")
        assertEquals(2, items.size)

        val asura = items[0]
        assertEquals("1001", asura.id)
        assertEquals("Asura Scans", asura.name)
        assertEquals("https://asuracomic.net", asura.baseUrl)
        assertEquals(ExtensionType.COMIC, asura.type)
        assertEquals(ExtensionEcosystem.MANGAYOMI, asura.ecosystem)
        assertEquals("1.0.4", asura.version)
        assertEquals("https://asuracomic.net/favicon.ico", asura.iconUrl)

        val kolnovel = items[1]
        assertEquals("1003", kolnovel.id)
        assertEquals("KolNovel", kolnovel.name)
        assertEquals(ExtensionType.NOVEL, kolnovel.type)
        assertEquals(ExtensionEcosystem.MANGAYOMI, kolnovel.ecosystem)
    }

    @Test
    fun testParseLNReaderCatalog() {
        val json = """
            [
              {
                "id": "royalroad",
                "name": "Royal Road",
                "site": "https://www.royalroad.com/",
                "lang": "English",
                "version": "2.1.0",
                "url": "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v2.1.0/royalroad.js",
                "iconUrl": "https://www.royalroad.com/icons/favicon.ico"
              },
              {
                "id": "novelfull",
                "name": "NovelFull",
                "site": "https://novelfull.com/",
                "lang": "English",
                "version": "1.0.5",
                "url": "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v1.0.5/novelfull.js"
              }
            ]
        """.trimIndent()

        val items = parser.parseCatalog(json, "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins")
        assertEquals(2, items.size)

        val rr = items[0]
        assertEquals("royalroad", rr.id)
        assertEquals("Royal Road", rr.name)
        assertEquals("https://www.royalroad.com/", rr.baseUrl)
        assertEquals(ExtensionType.NOVEL, rr.type)
        assertEquals(ExtensionEcosystem.LNREADER, rr.ecosystem)
        assertEquals("https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v2.1.0/royalroad.js", rr.pkg)

        val nf = items[1]
        assertEquals("novelfull", nf.id)
        assertEquals(ExtensionType.NOVEL, nf.type)
        assertEquals(ExtensionEcosystem.LNREADER, nf.ecosystem)
    }

    @Test
    fun testParsePaperbackCatalog() {
        val json = """
            {
              "buildTime": "2026-11-13T12:00:00Z",
              "sources": [
                {
                  "id": "MangaDex",
                  "name": "MangaDex",
                  "version": "1.3.2",
                  "author": "Paperback Team",
                  "websiteBaseURL": "https://mangadex.org",
                  "icon": "icon.png",
                  "contentRating": "EVERYONE",
                  "language": "en"
                },
                {
                  "id": "AdultSource",
                  "name": "Adult Source",
                  "version": "0.9.1",
                  "websiteBaseURL": "https://adult.example.com",
                  "icon": "icon.png",
                  "contentRating": "ADULT",
                  "language": "ja"
                }
              ]
            }
        """.trimIndent()

        val items = parser.parseCatalog(json, "https://raw.githubusercontent.com/paperback-community/extensions-v0.9/repo")
        assertEquals(2, items.size)

        val md = items[0]
        assertEquals("MangaDex", md.id)
        assertEquals("MangaDex", md.name)
        assertEquals("https://mangadex.org", md.baseUrl)
        assertEquals("https://raw.githubusercontent.com/paperback-community/extensions-v0.9/repo/MangaDex/index.js", md.pkg)
        assertEquals("https://raw.githubusercontent.com/paperback-community/extensions-v0.9/repo/MangaDex/icon.png", md.iconUrl)
        assertEquals(ExtensionType.COMIC, md.type)
        assertEquals(ExtensionEcosystem.PAPERBACK, md.ecosystem)
        assertFalse(md.isNsfw)

        val adult = items[1]
        assertEquals("AdultSource", adult.id)
        assertEquals(ExtensionEcosystem.PAPERBACK, adult.ecosystem)
        assertTrue(adult.isNsfw)
    }

    @Test
    fun testParseNativeDokuhaCatalog() {
        val json = """
            [
              {
                "id": "custom-manga",
                "name": "Custom Manga",
                "version": "1.0.0",
                "lang": "en",
                "baseUrl": "https://custom.example.com",
                "type": "COMIC",
                "pkg": "custom.js",
                "sha256": "abc123"
              }
            ]
        """.trimIndent()

        val items = parser.parseCatalog(json, "https://extensions.dokuha.app")
        assertEquals(1, items.size)
        assertEquals("custom-manga", items[0].id)
        assertEquals("Custom Manga", items[0].name)
        assertEquals(ExtensionType.COMIC, items[0].type)
        assertEquals(ExtensionEcosystem.CUSTOM, items[0].ecosystem)
    }

    @Test
    fun testMalformedOrEmptyJsonReturnsEmptyList() {
        assertEquals(0, parser.parseCatalog("", "https://example.com").size)
        assertEquals(0, parser.parseCatalog("{}", "https://example.com").size)
        assertEquals(0, parser.parseCatalog("[]", "https://example.com").size)
        assertEquals(0, parser.parseCatalog("not a json", "https://example.com").size)
    }
}
