import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import FileIcon from '@/components/file-icon.vue';
import IconPro from '@/components/icon-pro.vue';
import SvgIcon from '@/components/svg-icon.vue';

describe('FileIcon', () => {
  const mountIcon = (filename: string, directory: boolean) =>
    mount(FileIcon, { props: { filename, directory }, global: { components: { IconPro, SvgIcon } } });

  it('renders folder icon for directory', () => {
    const wrapper = mountIcon('dir', true);
    expect(wrapper.find('.el-icon').exists()).toBe(true);
  });

  it('renders file icon by extension', () => {
    const wrapper = mountIcon('test.js', false);
    expect(wrapper.html()).toContain('icon-javascript');
  });

  it('falls back to binary icon for unknown extension', () => {
    const wrapper = mountIcon('test.xyz', false);
    expect(wrapper.html()).toContain('icon-binary');
  });
});
