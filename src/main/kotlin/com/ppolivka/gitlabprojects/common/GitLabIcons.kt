package com.ppolivka.gitlabprojects.common

import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon

/**
 * Class encapsulating all custom GitLab Project icons
 *
 * @author ppolivka
 * @since 28.10.2015
 */
object GitLabIcons {
    var gitLabIcon: Icon = getIcon(
        "/icons/gitLabSmall.png",
        GitLabIcons::class.java
    ) //    public static JBImageIcon loadingIcon = new JBImageIcon(ImageLoader.loadFromResource("/icons/loading.gif", GitLabIcons.class));
}
