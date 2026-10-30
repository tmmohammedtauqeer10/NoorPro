package com.noorpro.app.v119

import com.noorpro.app.audio.catalog.CcBayans
import com.noorpro.app.audio.catalog.FreeAudioCatalog
import com.noorpro.app.audio.catalog.OfficialChannels
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.islamic.audio.BayanLogic
import org.junit.Assert.*
import org.junit.Test

/** 1.1.9 owner requests: no Ajmal Raza Qadri channel, no CC audio, Nasheeds = own MAF channel only. */
class RemovalsTest {
    @Test fun noOfficialChannelLinkOuts() {
        assertTrue(OfficialChannels.all.isEmpty())
    }
    @Test fun noCreativeCommonsAnywhere() {
        assertTrue(CcBayans.all.isEmpty() && CcBayans.channels.isEmpty())
        val cc = setOf(LicenseType.CC_BY, LicenseType.CC_BY_SA, LicenseType.CC0)
        assertTrue(FreeAudioCatalog.sources.none { it.license in cc })
    }
    @Test fun noThirdPartyNasheedFeeds() {
        assertTrue(PodcastRepository.podcasts.none { it.kind == "nasheed" })
    }
    @Test fun ajmalFeedsStayExcluded() {
        assertTrue(BayanLogic.EXCLUDED_TERMS.contains("ajmalrazaqadri"))
    }
}
