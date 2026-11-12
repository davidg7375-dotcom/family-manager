package com.family.manager

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*

class PopupActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val amt = intent.getDoubleExtra("a", 0.0)
        val mer = intent.getStringExtra("m") ?: ""
        val cats = arrayOf("כללי", "מזון", "דיור", "תחבורה", "בריאות", "קניית סחורה", "חינוך", "אחר")
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 32, 48, 16); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val ea = EditText(this).apply { setText(amt.toString()); inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val ed = EditText(this).apply { setText(mer); hint = "על מה ההוצאה?" }
        val sp = Spinner(this).apply { adapter = ArrayAdapter(this@PopupActivity, android.R.layout.simple_spinner_dropdown_item, cats) }
        val ok = Button(this).apply { text = "שמירה"; setOnClickListener {
            Core.savePending(this@PopupActivity, ea.text.toString().toDoubleOrNull() ?: amt, ed.text.toString(), sp.selectedItem.toString()); finish() } }
        val no = Button(this).apply { text = "זו לא הוצאה"; setOnClickListener { finish() } }
        l.addView(TextView(this).apply { text = "זוהתה הוצאה"; textSize = 18f })
        listOf(ea, ed, sp, ok, no).forEach { l.addView(it) }
        setContentView(l)
    }
}
