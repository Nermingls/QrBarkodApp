package dev.nermingules.qrbarkodapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import dev.nermingules.qrbarkodapp.databinding.FragmentMainScreenBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class MainScreenFragment : Fragment() {

    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var myViewModel: MyViewModel
    private var information: String? = null
    private var fisBilgisi: FisBilgisi? = null // FisBilgisi sınıf düzeyinde tanımlı


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        myViewModel.myData.observe(viewLifecycleOwner, Observer {
            information = it
            createFisBilgisi()
        })

        binding.qrOkut.setOnClickListener {
            val action = MainScreenFragmentDirections.actionMainScreenFragmentToQrFragment2()
            Navigation.findNavController(view).navigate(action)
        }

        binding.barcodOkut.setOnClickListener {
            val action = MainScreenFragmentDirections.actionMainScreenFragmentToSoruFragment2()
            Navigation.findNavController(view).navigate(action)
        }

        binding.send.setOnClickListener {
            val emailTextType = prepareEmailText(fisBilgisi!!)
            sendEmail(emailTextType)
            Post()
        }
    }


    private fun sendEmail(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"
        val mail = "nermingules1@gmail.com"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, "E-posta gönder"))
    }

    private fun parseFisBilgisiFromText(text: String): FisBilgisi {
        val tarihPattern = Regex("""TARIH\s*:\s*(\d{2}\.\d{2}\.\d{4})""")
        val fisNoPattern = Regex("""FiŞ NO\s*:\s*(\d{4})""")
        val toplamKVDPattern = Regex("""TOPKDV\s*[*:]?\s*([\d,.]+)""")
        val toplamTutarPattern = Regex("""TOPLAM\s*[*:]?\s*([\d,.]+)""")

        val tarihMatch = tarihPattern.find(text)
        val fisNoMatch = fisNoPattern.find(text)
        val toplamKVDMatch = toplamKVDPattern.find(text)
        val toplamTutarMatch = toplamTutarPattern.find(text)

        val tarih = tarihMatch?.groupValues?.get(1) ?: ""
        val fisNo = fisNoMatch?.groupValues?.get(1) ?: ""
        val toplamKDVString = toplamKVDMatch?.groupValues?.get(1) ?: "0.0"
        val toplamKDV = toplamKDVString.replace(",", ".").toDoubleOrNull() ?: 0.0
        val toplamTutarString = toplamTutarMatch?.groupValues?.get(1) ?: "0.0"
        val toplamTutar = toplamTutarString.replace(",", ".").toDoubleOrNull() ?: 0.0

        // ViewModel'daki değerleri al
        val myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        var fisTuru = ""
        var VCKN = ""

        myViewModel.myFisTur.observe(viewLifecycleOwner, Observer {
            fisTuru = it ?: ""
        })

        myViewModel.myVCKN.observe(viewLifecycleOwner, Observer {
            VCKN = it ?: ""
        })

        // Geri dönen FisBilgisi nesnesi artık gerekli değil
        return FisBilgisi(tarih, fisNo, toplamKDV, toplamTutar, fisTuru, VCKN)
    }

    private fun createFisBilgisi() {
        information?.let { visionAPIMetin ->
            fisBilgisi = parseFisBilgisiFromText(visionAPIMetin)
            fisBilgisi?.let {
                binding.fisNo.text = fisBilgisi!!.fisNo
                binding.tarih.text = fisBilgisi!!.tarih
                binding.total.text = fisBilgisi!!.toplamTutar.toString()
                binding.totalkdv.text = fisBilgisi!!.toplamKDV.toString()
            }
        }
    }
    private fun prepareEmailText(fisBilgisi: FisBilgisi): String {
        return """
        {
            "tarih":"${fisBilgisi.tarih}",
            "fisNo":"${fisBilgisi.fisNo}",
            "totalkdv":"${fisBilgisi.toplamKDV}",
            "total":"${fisBilgisi.toplamTutar}",
            "fisTuru":"${fisBilgisi.fisTuru}",
            "VCKN":"${fisBilgisi.VCKN}",
        }
    """.trimIndent()
    }

    fun Post() {

        fisBilgisi?.let {
            // JSON verisi
         val jsonData = prepareEmailText(fisBilgisi!!) // yourFisBilgisi değişkeni FisBilgisi türündeki veriyi temsil etmeli


        // JSON verisini istek gövdesine dönüştürme
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonData.toRequestBody(jsonMediaType)

        // HTTP isteği oluşturma
        val client = OkHttpClient()
        val url = "http://127.0.0.1:5000/api/BillDetails/get_totals-bill_detail" // Backend URL'si
        val request = Request.Builder()
            .url(url)
            .post(body) //header baslık auth 'na token barear
            .build()

// İstek gönderme ve yanıt alımı
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // İstek başarısız olduğunda burası çalışır
                e.printStackTrace()
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                // İstek başarılı olduğunda burası çalışır
                Toast.makeText(context,"Backende veri atıldı",Toast.LENGTH_LONG).show()
                val responseData = response.body?.string()
                // Backend'den gelen yanıtı kullanma işlemleri burada yapılabilir
                Toast.makeText(context,"Backenden gelen veri işlendi",Toast.LENGTH_LONG).show()
            }
        })
    }
    }
}
