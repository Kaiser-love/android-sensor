package com.wdy.sensor.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.kongzue.dialog.listener.OnMenuItemClickListener;
import com.kongzue.dialog.v2.BottomMenu;
import com.kongzue.dialog.v2.Pop;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.List;

import static com.kongzue.dialog.v2.Pop.COLOR_TYPE_ERROR;
import static com.kongzue.dialog.v2.Pop.SHOW_UP;

/**
 * @author dongyang_wu
 * @date 2019/7/28 01:39
 */
public class DialogUHelper {

    public static void shopTips(Context context, View view, String content, Long dismissTime) {
        Pop pop = Pop.show(context, view, content, SHOW_UP, COLOR_TYPE_ERROR);
        new Handler().postDelayed(pop::dismiss, dismissTime);
    }

    public static void shopSingleCheckableDialog(Context context, String[] items, Integer index, DialogInterface.OnClickListener callback) {
        new QMUIDialog.CheckableDialogBuilder(context)
                .setCheckedIndex(index)
                .addItems(items, callback)
                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show();
    }

    public static void shopBottomSheets(Context context, List<String> items, OnMenuItemClickListener callback) {
        BottomMenu.show((AppCompatActivity) context, items, callback, true);
//        BottomMenu.show((AppCompatActivity) context, items, new OnMenuItemClickListener() {
//            @Override
//            public void onClick(String text, int index) {
//                System.out.println(index);
//            }
//        }, true);
    }
}
