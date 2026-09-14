package com.leohao.android.alistlite.window;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kyleduo.switchbutton.SwitchButton;
import com.leohao.android.alistlite.R;
import com.leohao.android.alistlite.userscript.Userscript;
import com.leohao.android.alistlite.userscript.UserscriptManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 油猴脚本管理弹框：列表、启用开关、导入、粘贴新建、编辑、删除、刷新。
 *
 * @author AListLite 自编译分支
 */
public class UserscriptManagerDialog {
    /**
     * 导入脚本的文件选择请求码
     */
    public static final int REQUEST_CODE_IMPORT = 0x2A01;

    private static UserscriptManagerDialog instance;

    private final Activity activity;
    private final AlertDialog dialog;
    private final List<Userscript> scripts = new ArrayList<>();
    private final ScriptAdapter adapter;
    private final TextView dirTextView;

    private UserscriptManagerDialog(Activity activity) {
        this.activity = activity;
        LayoutInflater inflater = LayoutInflater.from(activity);
        View contentView = inflater.inflate(R.layout.userscript_manager_view, null);
        this.dirTextView = contentView.findViewById(R.id.tv_userscript_dir);

        ListView listView = contentView.findViewById(R.id.lv_userscript);
        this.adapter = new ScriptAdapter();
        listView.setAdapter(adapter);
        listView.setEmptyView(contentView.findViewById(R.id.tv_userscript_empty));
        listView.setOnItemClickListener((parent, view, position, id) -> showScriptActions(position));

        contentView.findViewById(R.id.btn_userscript_import).setOnClickListener(v -> importScript());
        contentView.findViewById(R.id.btn_userscript_new).setOnClickListener(v -> editScript(null, true));
        contentView.findViewById(R.id.btn_userscript_refresh).setOnClickListener(v -> {
            reload();
            Toast.makeText(activity, "已刷新", Toast.LENGTH_SHORT).show();
        });

        this.dialog = new AlertDialog.Builder(activity, R.style.IOSAlertDialog).create();
        dialog.setView(contentView);
        dialog.setOnDismissListener(d -> {
            if (instance == this) {
                instance = null;
            }
        });
    }

    /**
     * 打开脚本管理弹框
     */
    public static void show(Activity activity) {
        dismiss();
        instance = new UserscriptManagerDialog(activity);
        instance.display();
    }

    /**
     * 关闭脚本管理弹框
     */
    public static void dismiss() {
        if (instance != null) {
            instance.dialog.dismiss();
            instance = null;
        }
    }

    /**
     * 处理「导入脚本」的文件选择结果
     *
     * @return 是否已处理该结果
     */
    public static boolean handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_IMPORT) {
            return false;
        }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true;
        }
        Uri uri = data.getData();
        try {
            String content = readText(activity, uri);
            String displayName = queryDisplayName(activity, uri);
            String fileName = displayName == null || displayName.isEmpty()
                    ? UserscriptManager.getInstance().buildFileName(content)
                    : UserscriptManager.normalizeFileName(displayName);
            UserscriptManager.getInstance().saveScript(fileName, content);
            Toast.makeText(activity, "已导入：" + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "导入失败: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        if (instance != null) {
            instance.reload();
        }
        return true;
    }

    private void display() {
        reload();
        dialog.show();
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        if (width < height) {
            dialog.getWindow().setLayout(width - 50, height * 3 / 5);
        } else {
            dialog.getWindow().setLayout(width * 5 / 6, height - 200);
        }
    }

    private void reload() {
        scripts.clear();
        scripts.addAll(UserscriptManager.getInstance().loadScripts());
        adapter.notifyDataSetChanged();
        String dir = UserscriptManager.getInstance().getScriptDir().getAbsolutePath();
        dirTextView.setText(String.format("脚本目录：%s\n放入 .user.js 后点「刷新」；点击条目可编辑或删除。", dir));
    }

    private void showScriptActions(int position) {
        if (position < 0 || position >= scripts.size()) {
            return;
        }
        final Userscript script = scripts.get(position);
        new AlertDialog.Builder(activity, R.style.IOSAlertDialog)
                .setTitle(script.getName())
                .setItems(new String[]{"编辑内容", "删除脚本"}, (dialogInterface, which) -> {
                    if (which == 0) {
                        editScript(script, false);
                    } else {
                        confirmDelete(script);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(final Userscript script) {
        new AlertDialog.Builder(activity, R.style.IOSAlertDialog)
                .setTitle("删除脚本")
                .setMessage(String.format("确定删除「%s」吗？文件会从脚本目录中移除。", script.getName()))
                .setPositiveButton("删除", (dialogInterface, which) -> {
                    boolean deleted = UserscriptManager.getInstance().deleteScript(script);
                    Toast.makeText(activity, deleted ? "已删除，刷新页面后生效" : "删除失败",
                            Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 新建 / 编辑脚本
     */
    private void editScript(final Userscript script, final boolean isNew) {
        final EditText editText = new EditText(activity);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setTextSize(12f);
        editText.setGravity(Gravity.TOP | Gravity.START);
        editText.setMinLines(12);
        editText.setText(isNew ? UserscriptManager.getInstance().buildTemplate("") : script.getCode());
        editText.setSelection(editText.getText().length());

        FrameLayout container = new FrameLayout(activity);
        int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(editText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog dialog = new AlertDialog.Builder(activity, R.style.IOSAlertDialog)
                .setTitle(isNew ? "粘贴新建脚本" : "编辑脚本")
                .setView(container)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        if (width < height) {
            dialog.getWindow().setLayout(width - 50, height * 4 / 5);
        } else {
            dialog.getWindow().setLayout(width * 5 / 6, height - 200);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = editText.getText().toString();
            String fileName = isNew ? UserscriptManager.getInstance().buildFileName(content) : script.getId();
            try {
                UserscriptManager.getInstance().saveScript(fileName, content);
            } catch (Exception e) {
                Toast.makeText(activity, "保存失败: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(activity, "已保存，刷新页面后生效", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            reload();
        });
    }

    private void importScript() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            activity.startActivityForResult(Intent.createChooser(intent, "选择脚本文件"), REQUEST_CODE_IMPORT);
        } catch (Exception e) {
            Toast.makeText(activity, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    private static String readText(Activity activity, Uri uri) throws Exception {
        InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IllegalStateException("无法读取所选文件");
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    private static String queryDisplayName(Activity activity, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = activity.getContentResolver()
                    .query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
            // 查询失败时回退到按 @name 生成文件名
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 脚本列表适配器
     */
    private class ScriptAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return scripts.size();
        }

        @Override
        public Object getItem(int position) {
            return scripts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(R.layout.userscript_item_view, parent, false);
                holder = new ViewHolder();
                holder.nameTextView = convertView.findViewById(R.id.tv_userscript_name);
                holder.metaTextView = convertView.findViewById(R.id.tv_userscript_meta);
                holder.enableSwitch = convertView.findViewById(R.id.sb_userscript_enabled);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Userscript script = scripts.get(position);
            holder.nameTextView.setText(script.getName());
            StringBuilder meta = new StringBuilder();
            if (!script.getVersion().isEmpty()) {
                meta.append("v").append(script.getVersion()).append("  ");
            }
            meta.append(script.getMatchSummary());
            holder.metaTextView.setText(meta.toString());

            // 列表复用时先摘掉监听，避免滚动时误改其它脚本的开关状态
            holder.enableSwitch.setOnCheckedChangeListener(null);
            holder.enableSwitch.setCheckedNoEvent(script.isEnabled());
            holder.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                UserscriptManager.getInstance().setEnabled(script.getId(), isChecked);
                Toast.makeText(activity, isChecked ? "已启用，刷新页面后生效" : "已停用，刷新页面后生效",
                        Toast.LENGTH_SHORT).show();
            });
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView metaTextView;
        SwitchButton enableSwitch;
    }
}
