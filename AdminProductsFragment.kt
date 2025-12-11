package com.example.petshop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminProductsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_products, container, false)

        db = FirebaseFirestore.getInstance()

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_admin_products)
        val fab: FloatingActionButton = view.findViewById(R.id.fab_add_product)

        // Setup RecyclerView with a grid layout
        recyclerView.layoutManager = GridLayoutManager(context, 2) // Use 2 columns
        adapter = ProductAdapter(productList, true) // Pass true for admin
        recyclerView.adapter = adapter

        fetchProducts()

        // Setup FAB
        fab.setOnClickListener {
            val intent = Intent(activity, AddProductActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun fetchProducts() {
        db.collection("products")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AdminProducts", "Listen failed.", e)
                    return@addSnapshotListener
                }

                productList.clear()
                for (doc in snapshots!!) {
                    val product = doc.toObject(Product::class.java).copy(id = doc.id)
                    productList.add(product)
                }
                adapter.notifyDataSetChanged()
            }
    }
}