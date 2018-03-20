package com.github.dapeng.registry;

import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月20日 下午5:57
 */
public interface ChildListener {

    void childChanged(String path, List<String> children);

}
