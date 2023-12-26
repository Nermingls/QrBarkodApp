package dev.nermingules.qrbarkodapp

data class FisBilgisi(
    val tarih: String,
    val fisNo: String,
    val toplamKDV: Double,
    val toplamTutar: Double,
    val fisTuru: String,
    val VCKN: String
    // Diğer gerekli bilgiler
)