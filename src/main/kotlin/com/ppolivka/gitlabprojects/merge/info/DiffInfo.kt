package com.ppolivka.gitlabprojects.merge.info

import git4idea.util.GitCommitCompareInfo

/**
 * Class containing info about diff
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class DiffInfo(val info: GitCommitCompareInfo, val from: String, val to: String)
