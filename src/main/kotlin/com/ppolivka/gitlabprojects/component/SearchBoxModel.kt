package com.ppolivka.gitlabprojects.component

import com.ppolivka.gitlabprojects.merge.request.EmptyUser
import com.ppolivka.gitlabprojects.merge.request.SearchableUser
import com.ppolivka.gitlabprojects.merge.request.SearchableUsers
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.concurrent.Volatile

/**
 * Searchable ComboBox model with autocomplete and background loading
 *
 * @author ppolivka
 * @since 1.4.0
 */
class SearchBoxModel(private val comboBox: JComboBox<*>, searchableUsers: SearchableUsers) :
    AbstractListModel<Any?>(), ComboBoxModel<Any?>, KeyListener, ItemListener {
    @Transient
    private val comboBoxEditor: ComboBoxEditor = comboBox.editor
    private val searchableUsers: SearchableUsers

    private var data: MutableList<SearchableUser?> = ArrayList()
    private var selectedUser: SearchableUser? = null

    @Volatile
    private var lastKeyPressTime = 0L

    @Transient
    private val timer: Timer

    private var lastQuery = ""

    init {
        comboBoxEditor.editorComponent.addKeyListener(this)
        this.searchableUsers = searchableUsers
        data.addAll(searchableUsers.initialModel!!)
        timer = Timer()
    }

    private fun updateModel(`in`: String?) {
        timer.schedule(object : TimerTask() {
            override fun run() {
                SwingUtilities.invokeLater {
                    if ((System.currentTimeMillis() - lastKeyPressTime) > 200) {
                        if (`in` != null && "" != `in` && `in` != lastQuery) {
                            data.clear()
                            data = Arrays.asList(EmptyUser(`in`), EmptyUser("loading..."))
                            dataChanged()
                            data = ArrayList()
                            data.add(EmptyUser(`in`))
                            data.addAll(searchableUsers.search(`in`)!!)
                            lastQuery = `in`
                            dataChanged()
                        } else if (`in` == null || "" == `in`) {
                            data.clear()
                            data.addAll(searchableUsers.initialModel!!)
                            dataChanged()
                        }
                    }
                }
            }
        }, 200)
    }

    private fun dataChanged() {
        super.fireContentsChanged(this, 0, data.size)
        comboBox.hidePopup()
        comboBox.showPopup()
        if (!data.isEmpty()) {
            comboBox.selectedIndex = 0
        }
    }

    override fun itemStateChanged(e: ItemEvent) {
        comboBoxEditor.item = e.item //to string was here
        comboBox.selectedItem = e.item
    }

    override fun keyTyped(e: KeyEvent) {
        //noop
    }

    override fun keyPressed(e: KeyEvent) {
        lastKeyPressTime = System.currentTimeMillis()
    }

    override fun keyReleased(e: KeyEvent) {
        val str = comboBoxEditor.item.toString()
        val jtf = comboBoxEditor.editorComponent as JTextField
        val currentPosition = jtf.caretPosition

        if (e.keyChar == KeyEvent.CHAR_UNDEFINED) {
            if (e.keyCode != KeyEvent.VK_ENTER) {
                comboBoxEditor.item = str
                jtf.caretPosition = currentPosition
            }
        } else if (e.keyCode == KeyEvent.VK_ENTER) {
            comboBox.setSelectedIndex(comboBox.selectedIndex)
        } else {
            updateModel(comboBox.editor.item.toString())
            comboBoxEditor.item = str
            jtf.caretPosition = currentPosition
        }
    }

    override fun setSelectedItem(anItem: Any) {
        if (anItem is SearchableUser) {
            this.selectedUser = anItem
        }
    }

    override fun getSelectedItem(): Any {
        return selectedUser?:""
    }

    override fun getSize(): Int {
        return data.size
    }

    override fun getElementAt(index: Int): Any? {
        return data[index]
    }
}
