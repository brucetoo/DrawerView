package com.brucetoo.drawerview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private DrawerLayout mDrawerLayout;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        findViewById(R.id.content_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
            }
        });

        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(new QuickAdapter<String>(this, R.layout.list_item, getListData()) {
            @Override
            protected void convert(BaseAdapterHelper helper, String item) {
                helper.setText(R.id.text_item, item);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Click drawer:" + position, Toast.LENGTH_SHORT).show();
            }
        });


        mDrawerLayout.setDragRatioListener(new DrawerLayout.DragRatioListener() {
            @Override
            public void onDragRatioChange(float ratio, View dragView) {
                //抛出dragView 在此可以做任意view动画
                dragView.setAlpha(ratio);
                dragView.setPivotX(dragView.getWidth() / 2);
                dragView.setPivotY(dragView.getHeight() / 2);
                dragView.setScaleX(ratio);
                dragView.setScaleY(ratio);
            }
        });

        mDrawerLayout.setDragMaxWidth((int) getResources().getDimension(R.dimen.drawer_max_width));
        mDrawerLayout.setMaskEnable(true);
        mDrawerLayout.setMaskColor(getResources().getColor(R.color.colorMask));
    }

    private List<String> getListData() {
        ArrayList data = new ArrayList();
        for (int i = 0; i < 20; i++) {
            data.add("drawer text:" + i);
        }
        return data;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen()) {
            mDrawerLayout.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    private void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen()) {
            mDrawerLayout.closeDrawer();
        } else {
            mDrawerLayout.openDrawer();
        }
    }

}
