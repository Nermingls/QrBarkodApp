package dev.nermingules.qrbarkodapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import dev.nermingules.qrbarkodapp.databinding.FragmentQrBinding
import dev.nermingules.qrbarkodapp.databinding.FragmentSoruBinding
import dev.nermingules.qrbarkodapp.databinding.FragmentTaramaBinding


class SoruFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private var _binding: FragmentSoruBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSoruBinding.inflate(inflater, container, false)
        val view = binding.root

        val spinner: Spinner = view.findViewById(R.id.spinner1)
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.numbers,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this


        binding.button.setOnClickListener {

            val fisVCKN = binding.editText.text.toString()
            val myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
            myViewModel.myVCKN.value = fisVCKN // ViewModel'a da aktarabilirsiniz
            val action =SoruFragmentDirections.actionSoruFragment2ToTaramaFragment()
            Navigation.findNavController(view).navigate(action)
        }

        return view
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val fisTur = parent?.getItemAtPosition(position).toString()

        val myViewModel = ViewModelProvider(requireActivity()).get(MyViewModel::class.java)
        myViewModel.myFisTur.value = fisTur // ViewModel'a da aktarabilirsiniz
        Toast.makeText(parent?.context, fisTur, Toast.LENGTH_SHORT).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Hiçbir şey yapılması gerekmeyebilir
    }

}