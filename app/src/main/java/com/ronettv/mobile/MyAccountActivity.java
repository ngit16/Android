package com.ronettv.mobile;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ronettv.adapter.MyAccountMenuAdapter;

public class MyAccountActivity extends Activity {

    static final String FRAG_TAG = "My Fragment";
    // private static final String TAG = MyAccountActivity.class.getName();
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);
        listView = (ListView) findViewById(R.id.a_my_acc_lv_menu);
        MyAccountMenuAdapter menuAdapter = new MyAccountMenuAdapter(this);
        listView.setAdapter(menuAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(0, true);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                switch (arg2) {
                    case 0:
                        Fragment myPackageFrag = new MyPakagesFragment();
                        FragmentTransaction transaction1 = getFragmentManager()
                                .beginTransaction();
                        transaction1.replace(R.id.a_my_acc_frag_container,
                                myPackageFrag, FRAG_TAG);
                        transaction1.commit();
                        break;

                    case 1:
                        Fragment myProfileFrag = new MyProfileFragment();
                        FragmentTransaction transaction2 = getFragmentManager()
                                .beginTransaction();
                        transaction2.replace(R.id.a_my_acc_frag_container,
                                myProfileFrag, FRAG_TAG);
                        transaction2.commit();
                        break;
                }
            }
        });
        Fragment myPackageFrag = new MyPakagesFragment();
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        transaction.add(R.id.a_my_acc_frag_container, myPackageFrag, FRAG_TAG);
        transaction.commit();
    }

    public void btnSubmit_onClick(View v) {
        Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
        if (frag instanceof MyPakagesFragment) {
            ((MyPakagesFragment) frag).btnSubmit_onClick(v);
        }

    }

    public void ChangePwd_onClick(View v) {
        Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
        if (frag instanceof MyProfileFragment) {
            ((MyProfileFragment) frag).ChangePwd_onClick(v);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
        if (frag instanceof MyPakagesFragment) {
            ((MyPakagesFragment) frag).onCameResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
        if (frag instanceof MyPakagesFragment) {
            ((MyPakagesFragment) frag).onBackPressed();
        } else
            super.onBackPressed();
    }

}