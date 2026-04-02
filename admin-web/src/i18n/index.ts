import { ref } from 'vue';
import zh from '@/lang/zh';

const DEFAULT_LOCALE = 'zh-CN' as const;
const LOCALE_STORAGE_KEY = 'washer-admin-locale';

type SupportedLocale = 'zh' | 'zh-CN';
type MessageTree = Record<string, unknown>;

const messages: Record<SupportedLocale, MessageTree> = {
  zh,
  'zh-CN': zh,
};

export const locale = ref<SupportedLocale>(DEFAULT_LOCALE);

export const initLocale = () => {
  locale.value = DEFAULT_LOCALE;

  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, DEFAULT_LOCALE);
    document.documentElement.lang = DEFAULT_LOCALE;
  }
};

const resolveMessage = (messageTree: MessageTree, path: string): string => {
  const result = path.split('.').reduce<unknown>((current, key) => {
    if (current && typeof current === 'object') {
      return (current as Record<string, unknown>)[key];
    }

    return undefined;
  }, messageTree);

  return typeof result === 'string' ? result : path;
};

export const t = (key: string) => {
  return resolveMessage(messages[locale.value] || messages[DEFAULT_LOCALE], key);
};
