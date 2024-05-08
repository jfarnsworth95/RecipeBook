package com.jaf.recipebook.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaf.recipebook.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ListRecipes#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListRecipes extends Fragment {

    public ListRecipes() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ListRecipes.
     */
    public static ListRecipes newInstance() {
        return new ListRecipes();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_recipes, container, false);
    }
}