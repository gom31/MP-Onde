import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.seoultech.onde.R
import com.seoultech.onde.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class AIRecommendationBottomSheetFragment : DialogFragment() {
    private val apiKey = " "
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ai_recommendation, container, false)
        val closeButton: ImageButton = view.findViewById(R.id.closeButton)

        closeButton.setOnClickListener{
            dismiss()
        }

        val crawledSentences: TextView = view.findViewById(R.id.crawledSentences)
        val askAiButton: Button = view.findViewById(R.id.askAiButton)
        val aiQuestionInput: EditText = view.findViewById(R.id.aiQuestionInput)
        val gptAnswer: TextView = view.findViewById(R.id.gptAnswer)

        // 처음에 크롤링해서 텍스트를 바로 표시하도록 설정
        fetchCrawledData(crawledSentences)

        // 버튼 클릭 시 크롤링 및 GPT 응답 요청
        askAiButton.setOnClickListener {
            val userQuestion = aiQuestionInput.text.toString().trim()

            // 크롤링 수행
            fetchCrawledData(crawledSentences)

            // GPT 질문
            if (userQuestion.isNotEmpty()) {
                fetchGPTResponse(userQuestion, gptAnswer)
            } else {
                Toast.makeText(context, "AI에게 질문을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // 가로: 화면 너비의 80%, 세로: 화면 높이의 80%
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }


    private fun fetchCrawledData(crawledSentences: TextView) {
        val call = RetrofitClient.instance.getCrawledData()
        call.enqueue(object : Callback<List<String>> {
            override fun onResponse(
                call: Call<List<String>>,
                response: Response<List<String>>
            ) {
                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()

                    // 랜덤으로 하나의 문장 선택
                    val randomText = if (data.isNotEmpty()) {
                        data.random() // List에서 랜덤으로 하나 선택
                    } else {
                        "추천 멘트가 없습니다." // 데이터가 없을 경우 기본 문구
                    }

                    crawledSentences.text = randomText
                } else {
                    Log.e("APIError", "Response not successful: ${response.code()} - ${response.message()}")
                    crawledSentences.text = "서버 응답 실패: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e("APIError", "Network error: ${t.message}", t)
                crawledSentences.text = "네트워크 오류: ${t.message}"
            }
        })
    }



    private fun fetchGPTResponse(userQuestion: String, gptAnswer: TextView) {
        val apiUrl = "https://api.openai.com/v1/chat/completions"
    //  val apiKey = " "
        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo") // 최신 모델 사용
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuestion)
                })
            })
            put("max_tokens", 100)
            put("temperature", 0.7)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    val gptResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()

                    withContext(Dispatchers.Main) {
                        gptAnswer.text = gptResponse
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "GPT 응답 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    companion object {
        fun newInstance(): AIRecommendationBottomSheetFragment {
            return AIRecommendationBottomSheetFragment()
        }
    }
}
