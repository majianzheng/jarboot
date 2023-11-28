<template>
  <el-dialog
    :width="680"
    :title="$t('SELECT_AVATAR')"
    :model-value="visible"
    destroy-on-close
    class="c-avatar-cutter"
    @dragstart="false"
    @close="emit('update:visible', false)"
    :close-on-click-modal="false"
    :close-on-press-escape="false">
    <div class="container g-popup-box">
      <div class="content">
        <div class="c-left" :class="{ 'c-left--doing': state.imgURL }">
          <div
            class="container-box"
            :style="{
              width: `${state.containerBoxData.width}px`,
              height: `${state.containerBoxData.height}px`,
            }">
            <img ref="$img" :src="state.imgURL" alt="image" />
            <div class="img-mask"></div>
            <div
              class="select-box"
              v-if="state.imgURL"
              @mousedown="onMouseDown($event, 'move', '')"
              :style="{
                top: `${state.selectData.top}px`,
                left: `${state.selectData.left}px`,
                width: `${state.selectData.width}px`,
                height: `${state.selectData.width}px`,
                'background-image': `url(${state.imgURL})`,
                'background-position': `${-state.selectData.left}px ${-state.selectData.top}px`,
                'background-size': `${state.containerBoxData.width}px ${state.containerBoxData.height}px`,
              }">
              <ul class="x-line">
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'top')"></li>
                <li></li>
                <li></li>
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'bottom')"></li>
              </ul>
              <ul class="y-line">
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'left')"></li>
                <li></li>
                <li></li>
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'right')"></li>
              </ul>
              <ul class="point">
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'top-left')"></li>
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'top-right')"></li>
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'bottom-left')"></li>
                <li @mousedown.stop="onMouseDown($event, 'stretch', 'bottom-right')"></li>
              </ul>
              <div class="cross"></div>
            </div>
          </div>
        </div>
        <div class="c-right">
          <div class="preview">
            <p>{{ $t('PREVIEW') }}</p>
            <canvas ref="$canvas" width="190" height="190" :class="{ 'canvas--doing': state.imgURL }">></canvas>
          </div>
          <el-button type="primary" class="btn-upload">{{ !state.imgURL ? $t('UPLOAD_IMG') : $t('RE_UPLOAD_IMG') }}</el-button>
          <input @change="fileChange" type="file" accept="image/*" />
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="onCancel" type="primary" plain>{{ $t('CANCEL') }}</el-button>
      <el-button @click="onEnter" type="primary">{{ $t('SAVE') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import CommonNotice from '@/common/CommonNotice';
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import CommonUtils from '@/common/CommonUtils';

const props = defineProps({
  returnType: {
    type: String,
    default: 'url',
  },
  visible: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'cancel'): void;
  (e: 'enter', value: any): void;
}>();

const state = reactive({
  imgURL: '',
  scaleRate: 1, // 图片缩放比
  minWidth: 20, // 选择框最小宽度
  containerBoxData: {
    width: 0,
    height: 0,
  },
  selectData: {
    top: 0,
    left: 0,
    width: 0,
    action: '', // 当前进行的操作
    direction: '',
    originPoint: { x: 0, y: 0 }, // 点击时所在位置
    selectLine: '', // 选择那一条边进行拉伸，为空则不是在拉伸
  },
});
const $canvas = ref<HTMLCanvasElement>();
const $img = ref<HTMLImageElement>();

onMounted(() => {
  state.imgURL = '';
  // 全局监听松开事件，放在在内容选择框外松开
  document.addEventListener('mouseup', onMouseUp);
  document.addEventListener('mousemove', onMouseMove);
});
onUnmounted(() => {
  document.removeEventListener('mouseup', onMouseUp);
  document.removeEventListener('mousemove', onMouseMove);
});

// 获取图片宽高
function getImgSize(url: string) {
  return new Promise(resolve => {
    let $img = document.createElement('img');
    $img.src = url;
    $img.style.opacity = '0';
    $img.addEventListener('error', () => {
      document.body.removeChild($img);
      resolve(false);
    });
    $img.addEventListener('load', () => {
      const data = {
        width: $img.naturalWidth,
        height: $img.naturalHeight,
      };
      document.body.removeChild($img);
      resolve(data);
    });
    document.body.appendChild($img);
  });
}

// 从base64转化为file文件
function base64ToFile(base64Str: string, fileName: string) {
  const params = base64Str.split(',') as any;
  const mime = params[0].match(/:(.*?)/)[1];
  const fileData = atob(params[1]); // 解码Base64
  let { length } = fileData;
  const uint8Array = new Uint8Array(length);
  while (length) {
    length -= 1;
    uint8Array[length] = fileData.charCodeAt(length);
  }
  return new File([uint8Array], fileName, { type: mime });
}

// 获取驼峰写法
function getCamelCase(text: string) {
  return text.replace(/-[a-z]+?/g, matchStr => matchStr[1].toUpperCase());
}

// 获取首单词大写
function getWord(text: string) {
  return text[0].toUpperCase() + text.substr(1);
}

// 鼠标点击
function onMouseDown(event: any, action: string, direction: string) {
  const { selectData } = state;
  selectData.action = action;
  selectData.direction = direction || '';
  selectData.originPoint = {
    x: event.clientX > 0 ? event.clientX : 0,
    y: event.clientY > 0 ? event.clientY : 0,
  };
}

// 鼠标松开
function onMouseUp() {
  const { selectData } = state;
  selectData.action = '';
  selectData.direction = '';
}

// 鼠标移动
function onMouseMove(event: any) {
  const { selectData, containerBoxData } = state;
  const { x, y } = selectData.originPoint;
  const moveX = event.clientX - x; // X轴移动的距离
  const moveY = event.clientY - y; // Y轴移动的距离
  if (selectData.action === 'move') {
    // 移动选择框
    doMove(selectData, containerBoxData, moveX, moveY);
  } else if (selectData.action === 'stretch') {
    // 拉伸选择框
    doStretch(selectData, containerBoxData, moveX, moveY);
  } else {
    return;
  }

  selectData.originPoint = {
    x: event.clientX > 0 ? event.clientX : 0,
    y: event.clientY > 0 ? event.clientY : 0,
  };

  setPreview();
}

// 选择框移动
function doMove(selectData: any, containerBoxData: any, moveX: number, moveY: number) {
  selectData.top += moveY;
  selectData.left += moveX;
  if (selectData.top < 0) {
    selectData.top = 0;
  }
  if (selectData.left < 0) {
    selectData.left = 0;
  }
  if (selectData.top + selectData.width > containerBoxData.height) {
    selectData.top = containerBoxData.height - selectData.width;
  }
  if (selectData.left + selectData.width > containerBoxData.width) {
    selectData.left = containerBoxData.width - selectData.width;
  }
}

// 选择框拉伸
function doStretch(selectData: any, containerBoxData: any, moveX: number, moveY: number) {
  const { minWidth } = state;

  // 获取溢出长度
  function getOverflowLength() {
    const overflowLeft = selectData.left < 0 ? -selectData.left : 0;
    const overflowTop = selectData.top < 0 ? -selectData.top : 0;
    const overflowRight =
      selectData.left + selectData.width > containerBoxData.width ? selectData.left + selectData.width - containerBoxData.width : 0;
    const overflowBottom =
      selectData.top + selectData.width > containerBoxData.height ? selectData.top + selectData.width - containerBoxData.height : 0;
    const overflowWidth = selectData.width < minWidth ? minWidth - selectData.width : 0;
    return Math.max(overflowLeft, overflowTop, overflowRight, overflowBottom, overflowWidth);
  }

  // 向左拉伸
  function doStretchLeft(action: string) {
    let space = moveX;
    space = action === 'preDo' ? space : -space;
    selectData.top += space / 2;
    selectData.left += space;
    selectData.width -= space;
  }

  function doStretchRight(action: string) {
    let space = moveX;
    space = action === 'preDo' ? space : -space;
    selectData.top -= space / 2;
    selectData.width += space;
  }

  function doStretchTop(action: string) {
    let space = moveY;
    space = action === 'preDo' ? space : -space;
    selectData.top += space;
    selectData.left += space / 2;
    selectData.width -= space;
  }

  function doStretchBottom(action: string) {
    let space = moveY;
    space = action === 'preDo' ? space : -space;
    selectData.left -= space / 2;
    selectData.width += space;
  }

  function doStretchTopLeft(action: string) {
    let space = Math.abs(moveX) > Math.abs(moveY) ? moveX : moveY;
    space = action === 'preDo' ? space : -space;
    selectData.top += space;
    selectData.left += space;
    selectData.width -= space;
  }

  function doStretchTopRight(action: string) {
    let space = Math.abs(moveX) > Math.abs(moveY) ? moveX : -moveY;
    space = action === 'preDo' ? space : -space;
    selectData.top -= space;
    selectData.width += space;
  }

  function doStretchBottomLeft(action: string) {
    let space = Math.abs(moveX) > Math.abs(moveY) ? moveX : -moveY;
    space = action === 'preDo' ? space : -space;
    selectData.left += space;
    selectData.width -= space;
  }

  function doStretchBottomRight(action: string) {
    let space = Math.abs(moveX) > Math.abs(moveY) ? moveX : moveY;
    space = action === 'preDo' ? space : -space;
    selectData.width += space;
  }

  const doStretchFun = (
    {
      doStretchLeft,
      doStretchRight,
      doStretchTop,
      doStretchBottom,
      doStretchTopLeft,
      doStretchTopRight,
      doStretchBottomLeft,
      doStretchBottomRight,
    } as any
  )[`doStretch${getWord(getCamelCase(selectData.direction))}`];

  doStretchFun('preDo');
  let overflowLength = getOverflowLength();
  if (overflowLength > 0) {
    doStretchFun('reset');
  }
}

// 设置预览图
function setPreview() {
  const { selectData, scaleRate } = state;
  if (!$canvas.value) {
    return;
  }
  const canvas = $canvas.value.getContext('2d');
  if (!canvas) {
    return;
  }
  canvas.clearRect(0, 0, 190, 190);
  canvas.drawImage(
    $img.value as any,
    Math.floor(selectData.left / scaleRate),
    Math.floor(selectData.top / scaleRate),
    selectData.width / scaleRate,
    selectData.width / scaleRate,
    0,
    0,
    190,
    190
  );
}

// 选择图片
function fileChange(event: any) {
  const fileObj = event.target.files[0];
  const reader = new FileReader();
  reader.onload = () => {
    const { selectData, containerBoxData } = state;
    state.imgURL = reader.result as string;
    getImgSize(state.imgURL).then((result: any) => {
      if (result.width > result.height) {
        // 350为外盒子宽高
        state.scaleRate = 350 / result.width;
        containerBoxData.width = 350;
        containerBoxData.height = Math.floor(result.height * state.scaleRate);
        selectData.top = 0;
        selectData.left = (350 - containerBoxData.height) / 2;
        selectData.width = containerBoxData.height;
      } else {
        state.scaleRate = 350 / result.height;
        containerBoxData.height = 350;
        containerBoxData.width = Math.floor(result.width * state.scaleRate);
        selectData.left = 0;
        selectData.top = (350 - containerBoxData.width) / 2;
        selectData.width = containerBoxData.width;
      }
      setPreview();
    });
  };
  if (fileObj) reader.readAsDataURL(fileObj);
}

// 确认
function onEnter() {
  if (state.imgURL) {
    if (!$canvas.value) {
      return;
    }
    if (props.returnType === 'url') {
      emit('enter', $canvas.value.toDataURL()); // 返回链接
    } else if (props.returnType === 'file') {
      emit('enter', base64ToFile($canvas.value.toDataURL(), 'avatar.png')); // 返回文件
    }
    emit('update:visible', false);
  } else {
    CommonNotice.error(CommonUtils.translate('TIP_UPLOAD_IMG'));
  }
}

// 取消
function onCancel() {
  emit('cancel');
  emit('update:visible', false);
}
</script>
<style lang="scss" scoped>
li,
ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.c-avatar-cutter {
  user-select: none;
  align-items: center;
  .container {
    padding: 20px;
    width: 642px;
    box-sizing: border-box;
    background-color: var(--el-bg-color);
  }
  .content {
    display: flex;
    height: 350px;
  }
}
.c-left {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 350px;
  height: 350px;
  border: var(--el-border);
  background-repeat: round;
  background-image: url(./imgs/empty.png);
  .container-box {
    position: relative;
  }
  .img-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0.3;
    background-color: var(--el-bg-color);
  }
  img {
    width: 100%;
    height: 100%;
  }
  .select-box {
    position: absolute;
    top: 0;
    left: 0;
    cursor: move;
  }
  .x-line,
  .y-line {
    display: flex;
    justify-content: space-between;
    position: absolute;
    width: 100%;
    height: 100%;
    li {
      position: relative;
      border: dashed 1px var(--el-border-color);
    }
    li:nth-child(1):before,
    li:nth-last-child(1):before {
      position: absolute;
      margin-left: -3px;
      margin-top: -3px;
      width: 6px;
      height: 6px;
      content: '';
      background-color: var(--el-bg-color);
    }
  }
  .x-line {
    flex-direction: column;
    li:nth-child(1) {
      cursor: n-resize;
      &:before {
        left: 50%;
      }
    }
    li:nth-last-child(1) {
      cursor: s-resize;
      &:before {
        left: 50%;
      }
    }
  }
  .y-line {
    li:nth-child(1) {
      cursor: w-resize;
      &:before {
        top: 50%;
      }
    }
    li:nth-last-child(1) {
      cursor: e-resize;
      &:before {
        top: 50%;
      }
    }
  }
  .point {
    width: 100%;
    height: 100%;
    li {
      position: absolute;
      margin-left: -3px;
      margin-top: -3px;
      width: 6px;
      height: 6px;
      content: '';
      cursor: crosshair;
      background-color: var(--el-bg-color);
    }
    li:nth-child(1) {
      top: 2px;
      left: 1px;
      cursor: nw-resize;
    }
    li:nth-child(2) {
      top: 2px;
      right: -2px;
      cursor: ne-resize;
    }
    li:nth-child(3) {
      bottom: -2px;
      left: 1px;
      cursor: sw-resize;
    }
    li:nth-child(4) {
      bottom: -2px;
      right: -2px;
      cursor: se-resize;
    }
  }
  .cross {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 6px;
    height: 6px;
    min-width: unset;
    margin-top: -3px;
    margin-left: -3px;
    background-size: cover;
    background-image: url(./imgs/icon-cancel.png);
  }
}
.c-left--doing {
  background-image: url(./imgs/empty--pure.png);
}
.c-right {
  flex: 1;
  position: relative;
  margin-left: 24px;
  font-size: 16px;
  color: #333333;
  text-align: center;
  .preview {
    padding: 0 16px 34px;
    border: var(--el-border);
    background-color: var(--el-bg-color);
    p {
      height: 30px;
      line-height: 30px;
    }
    canvas {
      display: block;
      width: 190px;
      height: 190px;
      border: var(--el-border);
      background-repeat: round;
      background-image: url(./imgs/empty.png);
    }
    .canvas--doing {
      background-image: url(./imgs/empty--pure.png);
    }
  }
  .btn-upload {
    margin-top: 20px;
    width: 100%;
    height: 42px;
    line-height: 42px;
    font-size: 18px;
  }
  input[type='file'] {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 42px;
    opacity: 0;
    background-color: var(--el-bg-color);
  }
}
</style>
