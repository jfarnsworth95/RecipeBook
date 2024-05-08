package com.jaf.recipebook.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaf.recipebook.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IsLoading#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IsLoading extends Fragment {

    public IsLoading() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment IsLoading.
     */
    public static IsLoading newInstance() {
        IsLoading fragment = new IsLoading();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_is_loading, container, false);
    }
}