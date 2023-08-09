<style lang="scss" scoped>
.vue3-cron-div {
  .el-input-number__decrease,
  .el-input-number__increase {
    top: 2px !important;
  }
  .language {
    position: absolute;
    right: 25px;
    z-index: 1;
  }
  .el-tabs {
    box-shadow: none;
  }
  .tabBody {
    overflow: auto;
    .el-row {
      margin: 20px 0;
      .long {
        .el-select {
          width: 350px;
        }
      }
      .el-input-number {
        width: 110px;
      }
    }
  }
  .myScroller {
    &::-webkit-scrollbar {
      /*滚动条整体样式*/
      width: 5px; /*高宽分别对应横竖滚动条的尺寸*/
      height: 1px;
    }
    &::-webkit-scrollbar-thumb {
      /*滚动条里面小方块*/
      border-radius: 10px;
      background-color: skyblue;
      background-image: -webkit-linear-gradient(
        45deg,
        rgba(255, 255, 255, 0.2) 25%,
        transparent 25%,
        transparent 50%,
        rgba(255, 255, 255, 0.2) 50%,
        rgba(255, 255, 255, 0.2) 75%,
        transparent 75%,
        transparent
      );
    }
    &::-webkit-scrollbar-track {
      /*滚动条里面轨道*/
      box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.2);
      background: #ededed;
      border-radius: 10px;
    }
  }
  .bottom {
    width: 100%;
    margin-top: 5px;
    display: flex;
    align-items: center;
    justify-content: space-around;
    .value {
      float: left;
      font-size: 14px;
      vertical-align: middle;
      span:nth-child(1) {
        color: red;
      }
    }
  }
}
</style>
<template>
  <div class="vue3-cron-div">
    <el-button v-if="languageSwitchShow" class="language" type="text" @click="state.language = state.language === 'en-US' ? 'zh-CN' : 'en-US'">{{
      state.language === 'en-US' ? 'zh-CN' : 'en-US'
    }}</el-button>
    <el-tabs type="border-card">
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Seconds.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.second.cronEvery" label="1">{{ state.text.Seconds.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.second.cronEvery" label="2"
              >{{ state.text.Seconds.interval[0] }}
              <el-input-number size="small" v-model="state.second.incrementIncrement" :min="1" :max="60"></el-input-number>
              {{ state.text.Seconds.interval[1] || '' }}
              <el-input-number size="small" v-model="state.second.incrementStart" :min="0" :max="59"></el-input-number>
              {{ state.text.Seconds.interval[2] || '' }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.second.cronEvery" label="3"
              >{{ state.text.Seconds.specific }}
              <el-select size="small" multiple v-model="state.second.specificSpecific">
                <el-option v-for="(val, index) in 60" :key="index" :value="val - 1">{{ val - 1 }}</el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.second.cronEvery" label="4"
              >{{ state.text.Seconds.cycle[0] }}
              <el-input-number size="small" v-model="state.second.rangeStart" :min="1" :max="60"></el-input-number>
              {{ state.text.Seconds.cycle[1] || '' }}
              <el-input-number size="small" v-model="state.second.rangeEnd" :min="0" :max="59"></el-input-number>
              {{ state.text.Seconds.cycle[2] || '' }}
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Minutes.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.minute.cronEvery" label="1">{{ state.text.Minutes.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.minute.cronEvery" label="2"
              >{{ state.text.Minutes.interval[0] }}
              <el-input-number size="small" v-model="state.minute.incrementIncrement" :min="1" :max="60"></el-input-number>
              {{ state.text.Minutes.interval[1] }}
              <el-input-number size="small" v-model="state.minute.incrementStart" :min="0" :max="59"></el-input-number>
              {{ state.text.Minutes.interval[2] || '' }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.minute.cronEvery" label="3"
              >{{ state.text.Minutes.specific }}
              <el-select size="small" multiple v-model="state.minute.specificSpecific">
                <el-option v-for="(val, index) in 60" :key="index" :value="val - 1">{{ val - 1 }}</el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.minute.cronEvery" label="4"
              >{{ state.text.Minutes.cycle[0] }}
              <el-input-number size="small" v-model="state.minute.rangeStart" :min="1" :max="60"></el-input-number>
              {{ state.text.Minutes.cycle[1] }}
              <el-input-number size="small" v-model="state.minute.rangeEnd" :min="0" :max="59"></el-input-number>
              {{ state.text.Minutes.cycle[2] }}
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Hours.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.hour.cronEvery" label="1">{{ state.text.Hours.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.hour.cronEvery" label="2"
              >{{ state.text.Hours.interval[0] }}
              <el-input-number size="small" v-model="state.hour.incrementIncrement" :min="0" :max="23"></el-input-number>
              {{ state.text.Hours.interval[1] }}
              <el-input-number size="small" v-model="state.hour.incrementStart" :min="0" :max="23"></el-input-number>
              {{ state.text.Hours.interval[2] }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.hour.cronEvery" label="3"
              >{{ state.text.Hours.specific }}
              <el-select size="small" multiple v-model="state.hour.specificSpecific">
                <el-option v-for="(val, index) in 24" :key="index" :value="val - 1">{{ val - 1 }}</el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.hour.cronEvery" label="4"
              >{{ state.text.Hours.cycle[0] }}
              <el-input-number size="small" v-model="state.hour.rangeStart" :min="0" :max="23"></el-input-number>
              {{ state.text.Hours.cycle[1] }}
              <el-input-number size="small" v-model="state.hour.rangeEnd" :min="0" :max="23"></el-input-number>
              {{ state.text.Hours.cycle[2] }}
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Day.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="1">{{ state.text.Day.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="2"
              >{{ state.text.Day.intervalWeek[0] }}
              <el-input-number size="small" v-model="state.week.incrementIncrement" :min="1" :max="7"></el-input-number>
              {{ state.text.Day.intervalWeek[1] }}
              <el-select size="small" v-model="state.week.incrementStart">
                <el-option v-for="(val, index) in 7" :key="index" :label="state.text.Week[val - 1]" :value="val"></el-option>
              </el-select>
              {{ state.text.Day.intervalWeek[2] }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="3"
              >{{ state.text.Day.intervalDay[0] }}
              <el-input-number size="small" v-model="state.day.incrementIncrement" :min="1" :max="31"></el-input-number>
              {{ state.text.Day.intervalDay[1] }}
              <el-input-number size="small" v-model="state.day.incrementStart" :min="1" :max="31"></el-input-number>
              {{ state.text.Day.intervalDay[2] }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.day.cronEvery" label="4"
              >{{ state.text.Day.specificWeek }}
              <el-select size="small" multiple v-model="state.week.specificSpecific">
                <el-option
                  v-for="(val, index) in 7"
                  :key="index"
                  :label="state.text.Week[val - 1]"
                  :value="['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'][val - 1]"></el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.day.cronEvery" label="5"
              >{{ state.text.Day.specificDay }}
              <el-select size="small" multiple v-model="state.day.specificSpecific">
                <el-option v-for="(val, index) in 31" :key="index" :value="val">{{ val }}</el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="6">{{ state.text.Day.lastDay }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="7">{{ state.text.Day.lastWeekday }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="8"
              >{{ state.text.Day.lastWeek[0] }}
              <el-select size="small" v-model="state.day.cronLastSpecificDomDay">
                <el-option v-for="(val, index) in 7" :key="index" :label="state.text.Week[val - 1]" :value="val"></el-option>
              </el-select>
              {{ state.text.Day.lastWeek[1] || '' }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="9">
              <el-input-number size="small" v-model="state.day.cronDaysBeforeEomMinus" :min="1" :max="31"></el-input-number>
              {{ state.text.Day.beforeEndMonth[0] }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="10"
              >{{ state.text.Day.nearestWeekday[0] }}
              <el-input-number size="small" v-model="state.day.cronDaysNearestWeekday" :min="1" :max="31"></el-input-number>
              {{ state.text.Day.nearestWeekday[1] }}
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.day.cronEvery" label="11"
              >{{ state.text.Day.someWeekday[0] }}
              <el-input-number size="small" v-model="state.week.cronNthDayNth" :min="1" :max="5"></el-input-number>
              <el-select size="small" v-model="state.week.cronNthDayDay">
                <el-option v-for="(val, index) in 7" :key="index" :label="state.text.Week[val - 1]" :value="val"></el-option>
              </el-select>
              {{ state.text.Day.someWeekday[1] }}
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Month.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.month.cronEvery" label="1">{{ state.text.Month.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.month.cronEvery" label="2"
              >{{ state.text.Month.interval[0] }}
              <el-input-number size="small" v-model="state.month.incrementIncrement" :min="0" :max="12"></el-input-number>
              {{ state.text.Month.interval[1] }}
              <el-input-number size="small" v-model="state.month.incrementStart" :min="0" :max="12"></el-input-number>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.month.cronEvery" label="3"
              >{{ state.text.Month.specific }}
              <el-select size="small" multiple v-model="state.month.specificSpecific">
                <el-option v-for="(val, index) in 12" :key="index" :label="val" :value="val"></el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.month.cronEvery" label="4"
              >{{ state.text.Month.cycle[0] }}
              <el-input-number size="small" v-model="state.month.rangeStart" :min="1" :max="12"></el-input-number>
              {{ state.text.Month.cycle[1] }}
              <el-input-number size="small" v-model="state.month.rangeEnd" :min="1" :max="12"></el-input-number>
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
      <el-tab-pane>
        <template #label>
          <span><i class="el-icon-date"></i> {{ state.text.Year.name }}</span>
        </template>
        <div class="tabBody myScroller" :style="{ 'max-height': maxHeight }">
          <el-row>
            <el-radio v-model="state.year.cronEvery" label="1">{{ state.text.Year.every }}</el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.year.cronEvery" label="2"
              >{{ state.text.Year.interval[0] }}
              <el-input-number size="small" v-model="state.year.incrementIncrement" :min="1" :max="99"></el-input-number>
              {{ state.text.Year.interval[1] }}
              <el-input-number size="small" v-model="state.year.incrementStart" :min="2018" :max="2118"></el-input-number>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio class="long" v-model="state.year.cronEvery" label="3"
              >{{ state.text.Year.specific }}
              <el-select size="small" filterable multiple v-model="state.year.specificSpecific">
                <el-option v-for="(val, index) in 100" :key="index" :label="2017 + val" :value="2017 + val"></el-option>
              </el-select>
            </el-radio>
          </el-row>
          <el-row>
            <el-radio v-model="state.year.cronEvery" label="4"
              >{{ state.text.Year.cycle[0] }}
              <el-input-number size="small" v-model="state.year.rangeStart" :min="2018" :max="2118"></el-input-number>
              {{ state.text.Year.cycle[1] }}
              <el-input-number size="small" v-model="state.year.rangeEnd" :min="2018" :max="2118"></el-input-number>
            </el-radio>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>
    <div class="bottom">
      <div class="value">
        <span> cron {{ $t('PREVIEW') }}: </span>
        <el-tag>{{ state.cron }}</el-tag>
      </div>
      <div class="buttonDiv">
        <el-button type="primary" size="small" @click.stop="handleChange">{{ state.text.Save }}</el-button>
        <el-button type="primary" size="small" @click="close">{{ state.text.Close }}</el-button>
      </div>
    </div>
  </div>
</template>
<script>
import Language from './language';
import { reactive, computed, toRefs, defineComponent } from 'vue';
export default defineComponent({
  name: 'cron-editor',
  props: {
    cronValue: {},
    i18n: {},
    maxHeight: {},
    languageSwitchShow: false,
  },
  setup(props, { emit }) {
    const { i18n } = toRefs(props);
    const state = reactive({
      language: i18n.value,
      second: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
      },
      minute: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
      },
      hour: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
      },
      day: {
        cronEvery: '1',
        incrementStart: 1,
        incrementIncrement: 1,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
        cronLastSpecificDomDay: 1,
        cronDaysBeforeEomMinus: 0,
        cronDaysNearestWeekday: 0,
      },
      week: {
        cronEvery: '1',
        incrementStart: 1,
        incrementIncrement: 1,
        specificSpecific: [],
        cronNthDayDay: 1,
        cronNthDayNth: 1,
      },
      month: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
      },
      year: {
        cronEvery: '1',
        incrementStart: 2017,
        incrementIncrement: 1,
        rangeStart: 0,
        rangeEnd: 0,
        specificSpecific: [],
      },
      output: {
        second: '',
        minute: '',
        hour: '',
        day: '',
        month: '',
        Week: '',
        year: '',
      },
      text: computed(() => Language[state.language || 'zh-CN']),
      secondsText: computed(() => {
        let seconds = '';
        let cronEvery = state.second.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            seconds = '*';
            break;
          case '2':
            seconds = state.second.incrementStart + '/' + state.second.incrementIncrement;
            break;
          case '3':
            state.second.specificSpecific.map(val => {
              seconds += val + ',';
            });
            seconds = seconds.slice(0, -1);
            break;
          case '4':
            seconds = state.second.rangeStart + '-' + state.second.rangeEnd;
            break;
        }
        return seconds;
      }),
      minutesText: computed(() => {
        let minutes = '';
        let cronEvery = state.minute.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            minutes = '*';
            break;
          case '2':
            minutes = state.minute.incrementStart + '/' + state.minute.incrementIncrement;
            break;
          case '3':
            state.minute.specificSpecific.map(val => {
              minutes += val + ',';
            });
            minutes = minutes.slice(0, -1);
            break;
          case '4':
            minutes = state.minute.rangeStart + '-' + state.minute.rangeEnd;
            break;
        }
        return minutes;
      }),
      hoursText: computed(() => {
        let hours = '';
        let cronEvery = state.hour.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            hours = '*';
            break;
          case '2':
            hours = state.hour.incrementStart + '/' + state.hour.incrementIncrement;
            break;
          case '3':
            state.hour.specificSpecific.map(val => {
              hours += val + ',';
            });
            hours = hours.slice(0, -1);
            break;
          case '4':
            hours = state.hour.rangeStart + '-' + state.hour.rangeEnd;
            break;
        }
        return hours;
      }),
      daysText: computed(() => {
        let days = '';
        let cronEvery = state.day.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            break;
          case '2':
          case '4':
          case '11':
            days = '?';
            break;
          case '3':
            days = state.day.incrementStart + '/' + state.day.incrementIncrement;
            break;
          case '5':
            state.day.specificSpecific.map(val => {
              days += val + ',';
            });
            days = days.slice(0, -1);
            break;
          case '6':
            days = 'L';
            break;
          case '7':
            days = 'LW';
            break;
          case '8':
            days = state.day.cronLastSpecificDomDay + 'L';
            break;
          case '9':
            days = 'L-' + state.day.cronDaysBeforeEomMinus;
            break;
          case '10':
            days = state.day.cronDaysNearestWeekday + 'W';
            break;
        }
        return days;
      }),
      weeksText: computed(() => {
        let weeks = '';
        let cronEvery = state.day.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
          case '3':
          case '5':
            weeks = '?';
            break;
          case '2':
            weeks = state.week.incrementStart + '/' + state.week.incrementIncrement;
            break;
          case '4':
            state.week.specificSpecific.map(val => {
              weeks += val + ',';
            });
            weeks = weeks.slice(0, -1);
            break;
          case '6':
          case '7':
          case '8':
          case '9':
          case '10':
            weeks = '?';
            break;
          case '11':
            weeks = state.week.cronNthDayDay + '#' + state.week.cronNthDayNth;
            break;
        }
        return weeks;
      }),
      monthsText: computed(() => {
        let months = '';
        let cronEvery = state.month.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            months = '*';
            break;
          case '2':
            months = state.month.incrementStart + '/' + state.month.incrementIncrement;
            break;
          case '3':
            state.month.specificSpecific.map(val => {
              months += val + ',';
            });
            months = months.slice(0, -1);
            break;
          case '4':
            months = state.month.rangeStart + '-' + state.month.rangeEnd;
            break;
        }
        return months;
      }),
      yearsText: computed(() => {
        let years = '';
        let cronEvery = state.year.cronEvery;
        switch (cronEvery.toString()) {
          case '1':
            years = '*';
            break;
          case '2':
            years = state.year.incrementStart + '/' + state.year.incrementIncrement;
            break;
          case '3':
            state.year.specificSpecific.map(val => {
              years += val + ',';
            });
            years = years.slice(0, -1);
            break;
          case '4':
            years = state.year.rangeStart + '-' + state.year.rangeEnd;
            break;
        }
        return years;
      }),
      cron: computed(() => {
        return `${state.secondsText || '*'} ${state.minutesText || '*'} ${state.hoursText || '*'} ${state.daysText || '*'} ${
          state.monthsText || '*'
        } ${state.weeksText || '?'} ${state.yearsText || '*'}`;
      }),
    });
    const getValue = () => {
      return state.cron;
    };
    const close = () => {
      emit('close');
    };
    const handleChange = () => {
      emit('change', state.cron);
      close();
    };
    const rest = data => {
      for (let i in data) {
        if (data[i] instanceof Object) {
          this.rest(data[i]);
        } else {
          switch (typeof data[i]) {
            case 'object':
              data[i] = [];
              break;
            case 'string':
              data[i] = '';
              break;
          }
        }
      }
    };
    return {
      state,
      getValue,
      close,
      handleChange,
      rest,
    };
  },
});
</script>
